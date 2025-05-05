import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class AllureReporter {
    private final AllureLifecycle lifecycle = Allure.getLifecycle();

    /**
     * Начинает новый тест и возвращает его уникальный идентификатор.
     *
     * @param testName Название теста
     * @return UUID теста
     */
    public String startTest(String testName) {
        String uuid = UUID.randomUUID().toString();
        TestResult testResult = new TestResult()
                .setUuid(uuid)
                .setName(testName)
                .setStart(System.currentTimeMillis());
        lifecycle.scheduleTestCase(testResult);
        lifecycle.startTestCase(uuid);
        return uuid;
    }

    /**
     * Добавляет шаг к тесту.
     *
     * @param testUuid UUID теста
     * @param stepName Название шага
     * @param status Статус шага (PASSED, FAILED, SKIPPED)
     */
    public void addStep(String testUuid, String stepName, Status status) {
        String stepUuid = UUID.randomUUID().toString();
        StepResult stepResult = new StepResult()
                .setName(stepName)
                .setStatus(status)
                .setStart(System.currentTimeMillis())
                .setStop(System.currentTimeMillis());
        lifecycle.startStep(testUuid, stepUuid, stepResult);
        lifecycle.stopStep(stepUuid);
    }

    /**
     * Завершает тест с указанным статусом.
     *
     * @param testUuid UUID теста
     * @param status Статус теста (PASSED, FAILED, SKIPPED)
     */
    public void finishTest(String testUuid, Status status) {
        lifecycle.updateTestCase(testUuid, testResult -> 
            testResult.setStatus(status).setStop(System.currentTimeMillis()));
        lifecycle.stopTestCase(testUuid);
        lifecycle.writeTestCase(testUuid);
    }

    /**
     * Завершает тест с ошибкой.
     *
     * @param testUuid UUID теста
     * @param errorMessage Сообщение об ошибке
     */
    public void failTest(String testUuid, String errorMessage) {
        lifecycle.updateTestCase(testUuid, testResult -> 
            testResult.setStatus(Status.FAILED)
                      .setStatusDetails(new StatusDetails().setMessage(errorMessage))
                      .setStop(System.currentTimeMillis()));
        lifecycle.stopTestCase(testUuid);
        lifecycle.writeTestCase(testUuid);
    }

    /**
     * Прикрепляет текстовые данные к тесту.
     *
     * @param testUuid UUID теста
     * @param name Название вложения
     * @param content Содержимое вложения
     */
    public void attachText(String testUuid, String name, String content) {
        try (InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            lifecycle.addAttachment(name, "text/plain", ".txt", stream);
        } catch (Exception e) {
            // Логирование ошибки, если необходимо
        }
    }
}

        public AutoTestStepModel build() {
            return step;
        }
    }
}
