

import ru.testit.client.api.AutoTestsApi;
import ru.testit.client.invoker.ApiClient;
import ru.testit.client.invoker.ApiException;
import ru.testit.client.invoker.Configuration;
import ru.testit.client.model.AutoTestPostModel;
import ru.testit.client.model.AutoTestStepModel;
import ru.testit.client.model.LabelPostModel;
import ru.testit.client.model.AutoTestModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Класс для удобного создания автотестов в TestIt TMS.
 */
public class TestItAutoTestCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestItAutoTestCreator.class);
    private final AutoTestsApi autoTestsApi;

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
            model.setSteps(steps);
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
            if (model.getSteps() == null) {
                model.setSteps(new ArrayList<>());
            }
            model.getSteps().add(step);
            return this;
        }

        /**
         * Устанавливает список шагов настройки.
         * @param setup Список шагов настройки.
         * @return Этот билдер.
         */
        public AutoTestBuilder withSetup(List<AutoTestStepModel> setup) {
            model.setSetup(setup);
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
            if (model.getSetup() == null) {
                model.setSetup(new ArrayList<>());
            }
            model.getSetup().add(step);
            return this;
        }

        /**
         * Устанавливает список шагов завершения.
         * @param teardown Список шагов завершения.
         * @return Этот билдер.
         */
        public AutoTestBuilder withTeardown(List<AutoTestStepModel> teardown) {
            model.setTeardown(teardown);
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
            if (model.getTeardown() == null) {
                model.setTeardown(new ArrayList<>());
            }
            model.getTeardown().add(step);
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
            model.setLabels(labels);
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
         * Создает автотест в TestIt TMS.
         * @return Модель созданного автотеста (AutoTestModel).
         * @throws ApiException Если запрос к API завершился ошибкой.
         */
        public AutoTestModel build() throws ApiException {
            if (model.getExternalId() == null || model.getProjectId() == null || model.getName() == null) {
                LOGGER.error("Отсутствуют обязательные поля: externalId, projectId, name");
                throw new IllegalStateException("Required fields are missing: externalId, projectId, name");
            }
            LOGGER.info("Создание автотеста с externalId: {}", model.getExternalId());
            try {
                AutoTestModel result = autoTestsApi.createAutoTest(model);
                LOGGER.info("Автотест успешно создан с ID: {}", result.getId());
                return result;
            } catch (ApiException e) {
                LOGGER.error("Ошибка при создании автотеста: {}", e.getMessage(), e);
                throw e;
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
         */
        public StepBuilder addSubStep(AutoTestStepModel subStep) {
            if (step.getSteps() == null) {
                step.setSteps(new ArrayList<>());
            }
            step.getSteps().add(subStep);
            return this;
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
