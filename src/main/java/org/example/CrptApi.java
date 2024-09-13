package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ReentrantLock lock;
    private final Condition condition;
    private final AtomicInteger requestCount;
    private final int requestLimit;
    private final long timeInterval;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
        this.requestCount = new AtomicInteger(0);
        this.requestLimit = requestLimit;
        this.timeInterval = timeUnit.toMillis(1);

        // Reset request count at fixed intervals
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            lock.lock();
            try {
                requestCount.set(0);
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }, timeInterval, timeInterval, TimeUnit.MILLISECONDS);
    }

    public void createDocument(Document document, String signature) throws JsonProcessingException, InterruptedException {
        // Serialize document to JSON
        String jsonDocument = objectMapper.writeValueAsString(document);

        // Ensure the request does not exceed rate limit
        lock.lock();
        try {
            while (requestCount.get() >= requestLimit) {
                condition.await();
            }
            requestCount.incrementAndGet();
        } finally {
            lock.unlock();
        }

        // Prepare and send HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception during request processing", e);
        }
    }

    // Inner Document class for representing document structure
    public static class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;

        // Inner Description class
        public static class Description {
            public String participantInn;
        }

        // Inner Product class
        public static class Product {
            public String certificate_document;
            public String certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public String production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;
        }
    }

    public static void main(String[] args) {
        try {
            CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);
            Document doc = new Document();
            doc.doc_id = "123";
            doc.doc_status = "NEW";
            doc.doc_type = "LP_INTRODUCE_GOODS";
            doc.importRequest = true;
            doc.owner_inn = "4455667788";
            doc.participant_inn = "1212121212";
            doc.producer_inn = "3434343434";
            doc.production_date = "2021-01-01";
            doc.production_type = "TYPE";
            doc.reg_date = "2021-01-01";
            doc.reg_number = "987654321";

            // Setting up the Description
            Document.Description description = new Document.Description();
            description.participantInn = "0123456789";
            doc.description = description;

            // Setting up the Products
            Document.Product product = new Document.Product();
            product.certificate_document = "doc";
            product.certificate_document_date = "2021-01-01";
            product.certificate_document_number = "123";
            product.owner_inn = "9988776655";
            product.producer_inn = "5544332211";
            product.production_date = "2021-01-01";
            product.tnved_code = "0000";
            product.uit_code = "1111";
            product.uitu_code = "2222";
            doc.products = new Document.Product[] { product };

            // Create document via the API
            api.createDocument(doc, "signature");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}