import ru.testit.client.api.AutoTestsApi;
import ru.testit.client.api.TestRunsApi;
import ru.testit.client.invoker.ApiClient;
import ru.testit.client.invoker.ApiException;
import ru.testit.client.invoker.Configuration;
import ru.testit.client.model.AutoTestFilterModel;
import ru.testit.client.model.AutoTestPostModel;
import ru.testit.client.model.AutoTestPutModel;
import ru.testit.client.model.AutoTestStepModel;
import ru.testit.client.model.LabelPostModel;
import ru.testit.client.model.AutoTestModel;
import ru.testit.client.model.CreateEmptyTestRunApiModel;
import ru.testit.client.model.TestRunV2ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Класс для удобного создания автотестов и тестовых прогонов в TestIt TMS.
 */
public class TestItAutoTestCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestItAutoTestCreator.class);
    private final AutoTestsApi autoTestsApi;
    private final TestRunsApi testRunsApi;

    /**
     * Конструктор, инициализирующий API-клиент для TestIt TMS.
     * @param tmsAddress Адрес инстанции TestIt TMS (например, https://tms.company.com).
     * @param privateToken Приватный токен для аутентификации в API.
     */
    public TestItAutoTestCreator(String tmsAddress, String privateToken) {
        LOGGER.info("Инициализация TestItAutoTestCreator с адресом: {}", tmsAddress);
        if (tmsAddress == null || privateToken == null) {
            throw new IllegalArgumentException("Адрес TMS и токен не могут быть null");
        }
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(tmsAddress);
        defaultClient.setApiKeyPrefix("PrivateToken");
        defaultClient.setApiKey(privateToken);
        autoTestsApi = new AutoTestsApi(defaultClient);
        testRunsApi = new TestRunsApi(defaultClient);
    }

    /**
     * Создает тестовый прогон для проекта.
     * @param projectId UUID проекта.
     * @param name Название прогона.
     * @return UUID созданного прогона.
     * @throws ApiException Если запрос к API завершился ошибкой.
     */
    public UUID createTestRun(UUID projectId, String name) throws ApiException {
        LOGGER.info("Создание тестового прогона для проекта {} с названием {}", projectId, name);
        CreateEmptyTestRunApiModel model = new CreateEmptyTestRunApiModel();
        model.setProjectId(projectId);
        model.setName(name);
        TestRunV2ApiResult response = testRunsApi.createEmpty(model);
        testRunsApi.startTestRun(response.getId());
        LOGGER.info("Создан тестовый прогон с ID: {}", response.getId());
        return response.getId();
    }

    /**
     * Создает билдер для построения автотеста.
     * @return Новый экземпляр AutoTestBuilder.
     */
    public AutoTestBuilder createAutoTest() {
        return new AutoTestBuilder(autoTestsApi);
    }

    /**
     * Внутренний класс-билдер для создания автотеста.
     */
    public static class AutoTestBuilder {
        private final AutoTestsApi autoTestsApi;
        private final AutoTestPostModel model = new AutoTestPostModel();

        public AutoTestBuilder(AutoTestsApi autoTestsApi) {
            this.autoTestsApi = autoTestsApi;
        }

        /**
         * Устанавливает внешний идентификатор автотеста.
         * @param externalId Уникальный идентификатор (например, "test1").
         * @return Этот билдер.
         */
        public AutoTestBuilder withExternalId(String externalId) {
            model.setExternalId(externalId);
            return this;
        }

        /**
         * Устанавливает идентификатор проекта.
         * @param projectId UUID проекта в TestIt TMS.
         * @return Этот билдер.
         */
        public AutoTestBuilder withProjectId(UUID projectId) {
            model.setProjectId(projectId);
            return this;
        }

        /**
         * Устанавливает имя автотеста.
         * @param name Имя автотеста (например, "Тест 1").
         * @return Этот билдер.
         */
        public AutoTestBuilder withName(String name) {
            model.setName(name);
            return this;
        }

        /**
         * Устанавливает пространство имен автотеста.
         * @param namespace Пространство имен (например, "com.example.tests").
         * @return Этот билдер.
         */
        public AutoTestBuilder withNamespace(String namespace) {
            model.setNamespace(namespace);
            return this;
        }

        /**
         * Устанавливает имя класса автотеста.
         * @param classname Имя класса (например, "TestClass").
         * @return Этот билдер.
         */
        public AutoTestBuilder withClassname(String classname) {
            model.setClassname(classname);
            return this;
        }

        /**
         * Устанавливает список шагов автотеста.
         * @param steps Список шагов (AutoTestStepModel).
         * @return Этот билдер.
         */
        public AutoTestBuilder withSteps(List<AutoTestStepModel> steps) {
            model.setSteps(steps != null ? new ArrayList<>(steps) : null);
            return this;
        }

        /**
         * Добавляет шаг к автотесту.
         * @param title Название шага.
         * @param description Описание шага.
         * @return Этот билдер.
         */
        public AutoTestBuilder addStep(String title, String description) {
            AutoTestStepModel step = new AutoTestStepModel();
            step.setTitle(title);
            step.setDescription(description);
            List<AutoTestStepModel> steps = model.getSteps();
            if (steps == null || steps.getClass().getName().contains("Unmodifiable")) {
                steps = new ArrayList<>();
                model.setSteps(steps);
            }
            steps.add(step);
            return this;
        }

        /**
         * Устанавливает список шагов настройки.
         * @param setup Список шагов настройки.
         * @return Этот билдер.
         */
        public AutoTestBuilder withSetup(List<AutoTestStepModel> setup) {
            model.setSetup(setup != null ? new ArrayList<>(setup) : null);
            return this;
        }

        /**
         * Добавляет шаг настройки к автотесту.
         * @param title Название шага настройки.
         * @param description Описание шага настройки.
         * @return Этот билдер.
         */
        public AutoTestBuilder addSetupStep(String title, String description) {
            AutoTestStepModel step = new AutoTestStepModel();
            step.setTitle(title);
            step.setDescription(description);
            List<AutoTestStepModel> setup = model.getSetup();
            if (setup == null || setup.getClass().getName().contains("Unmodifiable")) {
                setup = new ArrayList<>();
                model.setSetup(setup);
            }
            setup.add(step);
            return this;
        }

        /**
         * Устанавливает список шагов завершения.
         * @param teardown Список шагов завершения.
         * @return Этот билдер.
         */
        public AutoTestBuilder withTeardown(List<AutoTestStepModel> teardown) {
            model.setTeardown(teardown != null ? new ArrayList<>(teardown) : null);
            return this;
        }

        /**
         * Добавляет шаг завершения к автотесту.
         * @param title Название шага завершения.
         * @param description Описание шага завершения.
         * @return Этот билдер.
         */
        public AutoTestBuilder addTeardownStep(String title, String description) {
            AutoTestStepModel step = new AutoTestStepModel();
            step.setTitle(title);
            step.setDescription(description);
            List<AutoTestStepModel> teardown = model.getTeardown();
            if (teardown == null || teardown.getClass().getName().contains("Unmodifiable")) {
                teardown = new ArrayList<>();
                model.setTeardown(teardown);
            }
            teardown.add(step);
            return this;
        }

        /**
         * Устанавливает заголовок автотеста.
         * @param title Заголовок автотеста.
         * @return Этот билдер.
         */
        public AutoTestBuilder withTitle(String title) {
            model.setTitle(title);
            return this;
        }

        /**
         * Устанавливает описание автотеста.
         * @param description Описание автотеста.
         * @return Этот билдер.
         */
        public AutoTestBuilder withDescription(String description) {
            model.setDescription(description);
            return this;
        }

        /**
         * Устанавливает список меток автотеста.
         * @param labels Список меток (LabelPostModel).
         * @return Этот билдер.
         */
        public AutoTestBuilder withLabels(List<LabelPostModel> labels) {
            model.setLabels(labels != null ? new ArrayList<>(labels) : null);
            return this;
        }

        /**
         * Добавляет метку к автотесту.
         * @param name Название метки (например, "smoke").
         * @return Этот билдер.
         */
        public AutoTestBuilder addLabel(String name) {
            LabelPostModel label = new LabelPostModel();
            label.setName(name);
            if (model.getLabels() == null) {
                model.setLabels(new ArrayList<>());
            }
            model.getLabels().add(label);
            return this;
        }

        /**
         * Устанавливает флаг нестабильности автотеста.
         * @param isFlaky Флаг нестабильности (true, если тест нестабилен).
         * @return Этот билдер.
         */
        public AutoTestBuilder withIsFlaky(boolean isFlaky) {
            model.setIsFlaky(isFlaky);
            return this;
        }

        /**
         * Устанавливает внешний ключ автотеста.
         * @param externalKey Внешний ключ.
         * @return Этот билдер.
         */
        public AutoTestBuilder withExternalKey(String externalKey) {
            model.setExternalKey(externalKey);
            return this;
        }

        /**
         * Устанавливает список ID рабочих элементов для привязки к задачам (например, Jira).
         * @param workItemIds Список ID рабочих элементов.
         * @return Этот билдер.
         */
        public AutoTestBuilder withWorkItemIds(List<String> workItemIds) {
            model.setWorkItemIds(workItemIds);
            return this;
        }

        /**
         * Создает или обновляет автотест в TestIt TMS.
         * Если автотест с таким externalId существует, обновляет его; иначе создает новый.
         * @return Модель созданного или обновленного автотеста (AutoTestModel).
         * @throws ApiException Если запрос к API завершился ошибкой.
         */
        public AutoTestModel build() throws ApiException {
            if (model.getExternalId() == null || model.getProjectId() == null || model.getName() == null) {
                LOGGER.error("Отсутствуют обязательные поля: externalId, projectId, name");
                throw new IllegalStateException("Required fields are missing: externalId, projectId, name");
            }

            // Поиск существующего автотеста
            AutoTestFilterModel filter = new AutoTestFilterModel()
                    .externalIds(List.of(model.getExternalId()))
                    .projectIds(List.of(model.getProjectId()));
            List<AutoTestModel> existingTests = autoTestsApi.apiV2AutoTestsSearchPost(
                    null, null, null, null, null,
                    new ru.testit.client.model.SearchAutoTestsQueryIncludesModel().filter(filter)
            );

            if (!existingTests.isEmpty()) {
                // Обновление существующего автотеста
                AutoTestModel existing = existingTests.get(0);
                AutoTestPutModel putModel = new AutoTestPutModel();
                putModel.setId(existing.getId());
                putModel.setExternalId(model.getExternalId());
                putModel.setProjectId(model.getProjectId());
                putModel.setName(model.getName());
                putModel.setNamespace(model.getNamespace());
                putModel.setClassname(model.getClassname());
                putModel.setSteps(model.getSteps() != null ? new ArrayList<>(model.getSteps()) : null);
                putModel.setSetup(model.getSetup() != null ? new ArrayList<>(model.getSetup()) : null);
                putModel.setTeardown(model.getTeardown() != null ? new ArrayList<>(model.getTeardown()) : null);
                putModel.setTitle(model.getTitle());
                putModel.setDescription(model.getDescription());
                putModel.setLabels(model.getLabels() != null ? new ArrayList<>(model.getLabels()) : null);
                putModel.setIsFlaky(model.getIsFlaky());
                putModel.setExternalKey(model.getExternalKey());
                putModel.setWorkItemIds(model.getWorkItemIds());

                LOGGER.info("Обновление существующего автотеста с ID: {}", existing.getId());
                autoTestsApi.updateAutoTest(putModel);
                return existing; // Возвращаем существующий, так как обновление не возвращает модель
            } else {
                // Создание нового автотеста
                LOGGER.info("Создание нового автотеста с externalId: {}", model.getExternalId());
                AutoTestModel result = autoTestsApi.createAutoTest(model);
                LOGGER.info("Автотест создан с ID: {}", result.getId());
                return result;
            }
        }
    }

    /**
     * Внутренний класс-билдер для создания шагов автотеста.
     */
    public static class StepBuilder {
        private final AutoTestStepModel step = new AutoTestStepModel();

        /**
         * Устанавливает название шага.
         * @param title Название шага.
         * @return Этот билдер.
         */
        public StepBuilder withTitle(String title) {
            step.setTitle(title);
            return this;
        }

        /**
         * Устанавливает описание шага.
         * @param description Описание шага.
         * @return Этот билдер.
         */
        public StepBuilder withDescription(String description) {
            step.setDescription(description);
            return this;
        }

        /**
         * Добавляет подшаг к текущему шагу.
         * @param subStep Подшаг (AutoTestStepModel).
         * @return Этот билдер.
         * @throws IllegalArgumentException Если подшаг содержит циклическую ссылку.
         */
        public StepBuilder addSubStep(AutoTestStepModel subStep) {
            if (subStep == step) {
                throw new IllegalArgumentException("Нельзя добавить шаг к самому себе");
            }
            if (hasCycle(step, subStep)) {
                throw new IllegalArgumentException("Обнаружена циклическая ссылка в шагах");
            }
            if (step.getSteps() == null) {
                step.setSteps(new ArrayList<>());
            }
            step.getSteps().add(subStep);
            return this;
        }

        /**
         * Проверяет наличие циклической ссылки в иерархии шагов.
         * @param parent Родительский шаг.
         * @param child Проверяемый подшаг.
         * @return true, если есть цикл, false иначе.
         */
        private boolean hasCycle(AutoTestStepModel parent, AutoTestStepModel child) {
            if (child == null) return false;
            if (child == parent) return true;
            if (child.getSteps() == null) return false;
            for (AutoTestStepModel subStep : child.getSteps()) {
                if (hasCycle(parent, subStep)) return true;
            }
            return false;
        }

        /**
         * Создает модель шага.
         * @return Модель шага (AutoTestStepModel).
         */
        public AutoTestStepModel build() {
            return step;
        }
    }
}
        public AutoTestStepModel build() {
            return step;
        }
    }
}
