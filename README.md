import ru.testit.client.api.AutoTestsApi;
import ru.testit.client.api.TestRunsApi;
import ru.testit.client.api.WorkItemsApi;
import ru.testit.client.invoker.ApiClient;
import ru.testit.client.invoker.ApiException;
import ru.testit.client.invoker.Configuration;
import ru.testit.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Класс для удобного создания, обновления и удаления автотестов и тестовых прогонов в TestIt TMS.
 */
public class TestItAutoTestCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestItAutoTestCreator.class);
    private final AutoTestsApi autoTestsApi;
    private final TestRunsApi testRunsApi;
    private final WorkItemsApi workItemsApi;

    /**
     * Конструктор, инициализирующий API-клиент для TestIt TMS.
     * @param tmsAddress Адрес инстанции TestIt TMS (например, https://tms.company.com).
     * @param privateToken Приватный токен для аутентификации в API.
     */
    public TestItAutoTestCreator(String tmsAddress, String privateToken) {
        LOGGER.info("Инициализация TestItAutoTestCreator с адресом: {}", tmsAddress);
        if (tmsAddress == null || privateToken == null) {
            throw new IllegalArgumentException W("Адрес TMS и токен не могут быть null");
        }
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(tmsAddress);
        defaultClient.setApiKeyPrefix("PrivateToken");
        defaultClient.setApiKey(privateToken);
        autoTestsApi = new AutoTestsApi(defaultClient);
        testRunsApi = new TestRunsApi(defaultClient);
        workItemsApi = new WorkItemsApi(defaultClient);
    }

    /**
     * Создает тестовый прогон для проекта.
     * @param projectId UUID проекта.
     * @param name Название прогона.
     * @return Optional с UUID созданного прогона или пустой, если произошла ошибка.
     */
    public Optional<UUID> createTestRun(UUID projectId, String name) {
        LOGGER.info("Создание тестового прогона для проекта {} с названием {}", projectId, name);
        try {
            CreateEmptyTestRunApiModel model = new CreateEmptyTestRunApiModel();
            model.setProjectId(projectId);
            model.setName(name);
            TestRunV2ApiResult response = testRunsApi.createEmpty(model);
            testRunsApi.startTestRun(response.getId());
            LOGGER.info("Создан тестовый прогон с ID: {}", response.getId());
            return Optional.of(response.getId());
        } catch (ApiException e) {
            LOGGER.error("Ошибка при создании тестового прогона: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Удаляет автотест по его внешнему идентификатору.
     * @param externalId Внешний идентификатор автотеста.
     * @param projectId UUID проекта.
     */
    public void deleteAutoTestByExternalId(String externalId, UUID projectId) {
        LOGGER.info("Поиск автотеста с externalId: {} в проекте: {}", externalId, projectId);
        try {
            AutoTestFilterApiModel filter = new AutoTestFilterApiModel();
            filter.setExternalIds(Set.of(externalId));
            filter.setProjectIds(Set.of(projectId.toString()));
            AutoTestSearchIncludeApiModel includes = new AutoTestSearchIncludeApiModel()
                    .includeLabels(true)
                    .includeSteps(true)
                    .includeLinks(true);
            AutoTestSearchApiModel body = new AutoTestSearchApiModel()
                    .filter(filter)
                    .includes(includes);
            List<AutoTestApiResult> existingTests = autoTestsApi.apiV2AutoTestsSearchPost(
                    null, null, null, null, null, body
            );
            if (!existingTests.isEmpty()) {
                String id = existingTests.get(0).getId().toString();
                autoTestsApi.deleteAutoTest(id);
                LOGGER.info("Удален автотест с externalId: {}", externalId);
            } else {
                LOGGER.warn("Автотест с externalId: {} в проекте: {} не найден", externalId, projectId);
            }
        } catch (ApiException e) {
            LOGGER.error("Ошибка при удалении автотеста с externalId {}: {}", externalId, e.getMessage(), e);
        }
    }

    /**
     * Удаляет тестовый прогон по его ID.
     * @param testRunId UUID тестового прогона.
     */
    public void deleteTestRun(UUID testRunId) {
        LOGGER.info("Удаление тестового прогона с ID: {}", testRunId);
        try {
            testRunsApi.apiV2TestRunsIdDelete(testRunId.toString());
            LOGGER.info("Тестовый прогон успешно удален");
        } catch (ApiException e) {
            LOGGER.error("Ошибка при удалении тестового прогона с ID {}: {}", testRunId, e.getMessage(), e);
        }
    }

    /**
     * Находит ID рабочего элемента по его внешнему идентификатору (например, ключу задачи Jira).
     * @param projectId UUID проекта.
     * @param externalId Внешний идентификатор (например, "JIRA-123").
     * @return Optional с UUID рабочего элемента или пустой, если элемент не найден.
     */
    public Optional<UUID> getWorkItemIdByExternalId(UUID projectId, String externalId) {
        LOGGER.info("Поиск рабочего элемента с externalId: {} в проекте: {}", externalId, projectId);
        try {
            WorkItemFilterModel filter = new WorkItemFilterModel()
                    .projectIds(List.of(projectId))
                    .externalIds(List.of(externalId));
            SearchWorkItemsRequestV2Model body = new SearchWorkItemsRequestV2Model().filter(filter);
            List<WorkItemModel> workItems = workItemsApi.apiV2WorkItemsSearchPost(null, null, null, null, null, body);
            if (workItems.isEmpty()) {
                LOGGER.warn("Рабочий элемент с externalId: {} в проекте: {} не найден", externalId, projectId);
                return Optional.empty();
            }
            return Optional.of(workItems.get(0).getId());
        } catch (ApiException e) {
            LOGGER.error("Ошибка при поиске рабочего элемента с externalId {}: {}", externalId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Создает билдер для построения автотеста.
     * @return Новый экземпляр AutoTestBuilder.
     */
    public AutoTestBuilder createAutoTest() {
        return new AutoTestBuilder(autoTestsApi, this);
    }

    /**
     * Внутренний класс-билдер для создания автотеста.
     */
    public static class AutoTestBuilder {
        private final AutoTestsApi autoTestsApi;
        private final TestItAutoTestCreator creator;
        private final AutoTestPostModel model = new AutoTestPostModel();

        public AutoTestBuilder(AutoTestsApi autoTestsApi, TestItAutoTestCreator creator) {
            this.autoTestsApi = autoTestsApi;
            this.creator = creator;
        }

        public AutoTestBuilder withExternalId(String externalId) {
            model.setExternalId(externalId);
            return this;
        }

        public AutoTestBuilder withProjectId(UUID projectId) {
            model.setProjectId(projectId);
            return this;
        }

        public AutoTestBuilder withName(String name) {
            model.setName(name);
            return this;
        }

        public AutoTestBuilder withNamespace(String namespace) {
            model.setNamespace(namespace);
            return this;
        }

        public AutoTestBuilder withClassname(String classname) {
            model.setClassname(classname);
            return this;
        }

        public AutoTestBuilder withSteps(List<AutoTestStepModel> steps) {
            model.setSteps(steps != null ? new ArrayList<>(steps) : null);
            return this;
        }

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

        public AutoTestBuilder withSetup(List<AutoTestStepModel> setup) {
            model.setSetup(setup != null ? new ArrayList<>(setup) : null);
            return this;
        }

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

        public AutoTestBuilder withTeardown(List<AutoTestStepModel> teardown) {
            model.setTeardown(teardown != null ? new ArrayList<>(teardown) : null);
            return this;
        }

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

        public AutoTestBuilder withTitle(String title) {
            model.setTitle(title);
            return this;
        }

        public AutoTestBuilder withDescription(String description) {
            model.setDescription(description);
            return this;
        }

        public AutoTestBuilder withLabels(List<LabelPostModel> labels) {
            model.setLabels(labels != null ? new ArrayList<>(labels) : null);
            return this;
        }

        public AutoTestBuilder addLabel(String name) {
            LabelPostModel label = new LabelPostModel();
            label.setName(name);
            if (model.getLabels() == null) {
                model.setLabels(new ArrayList<>());
            }
            model.getLabels().add(label);
            return this;
        }

        public AutoTestBuilder withIsFlaky(boolean isFlaky) {
            model.setIsFlaky(isFlaky);
            return this;
        }

        public AutoTestBuilder withExternalKey(String externalKey) {
            model.setExternalKey(externalKey);
            return this;
        }

        /**
         * Привязывает автотест к задаче Jira по её ключу.
         * @param jiraTaskKey Ключ задачи Jira (например, "JIRA-123").
         * @return Этот билдер.
         */
        public AutoTestBuilder linkToJiraTask(String jiraTaskKey) {
            if (model.getProjectId() == null) {
                LOGGER.error("Не указан projectId для привязки к Jira задаче {}", jiraTaskKey);
                return this;
            }
            Optional<UUID> workItemId = creator.getWorkItemIdByExternalId(model.getProjectId(), jiraTaskKey);
            if (workItemId.isPresent()) {
                if (model.getWorkItemIds() == null) {
                    model.setWorkItemIds(new ArrayList<>());
                }
                model.getWorkItemIds().add(workItemId.get().toString());
                LOGGER.info("Привязка автотеста к Jira задаче {} (workItemId: {})", jiraTaskKey, workItemId.get());
            } else {
                LOGGER.warn("Не удалось привязать автотест к Jira задаче {}", jiraTaskKey);
            }
            return this;
        }

        /**
         * Добавляет ссылку к автотесту.
         * @param url URL ссылки (например, "https://your-jira-instance.com/browse/JIRA-123").
         * @param title Название ссылки (например, "JIRA-123").
         * @param type Тип ссылки (например, "Issue" для Jira задач).
         * @return Этот билдер.
         */
        public AutoTestBuilder addLink(String url, String title, String type) {
            if (url == null || title == null || type == null) {
                LOGGER.error("URL, title или type ссылки не могут быть null");
                return this;
            }
            LinkPostModel link = new LinkPostModel();
            link.setUrl(url);
            link.setTitle(title);
            link.setType(type);
            if (model.getLinks() == null) {
                model.setLinks(new ArrayList<>());
            }
            model.getLinks().add(link);
            LOGGER.info("Добавлена ссылка к автотесту: {} ({})", title, url);
            return this;
        }

        /**
         * Создает или обновляет автотест в TestIt TMS.
         * @return Optional с моделью созданного или обновленного автотеста или пустой, если произошла ошибка.
         */
        public Optional<AutoTestModel> build() {
            if (model.getExternalId() == null || model.getProjectId() == null || model.getName() == null) {
                LOGGER.error("Отсутствуют обязательные поля: externalId, projectId, name");
                return Optional.empty();
            }

            try {
                AutoTestFilterApiModel filter = new AutoTestFilterApiModel();
                filter.setExternalIds(Set.of(model.getExternalId()));
                filter.setProjectIds(Set.of(model.getProjectId().toString()));
                AutoTestSearchIncludeApiModel includes = new AutoTestSearchIncludeApiModel()
                        .includeLabels(true)
                        .includeSteps(true)
                        .includeLinks(true);
                AutoTestSearchApiModel body = new AutoTestSearchApiModel()
                        .filter(filter)
                        .includes(includes);
                List<AutoTestApiResult> existingTests = autoTestsApi.apiV2AutoTestsSearchPost(
                        null, null, null, null, null, body
                );

                if (!existingTests.isEmpty()) {
                    AutoTestApiResult existing = existingTests.get(0);
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
                    // Преобразование LinkPostModel в LinkPutModel
                    List<LinkPutModel> putLinks = model.getLinks() != null ? model.getLinks().stream()
                            .map(link -> {
                                LinkPutModel putLink = new LinkPutModel();
                                putLink.setUrl(link.getUrl());
                                putLink.setTitle(link.getTitle());
                                putLink.setType(link.getType());
                                return putLink;
                            })
                            .collect(Collectors.toList()) : null;
                    putModel.setLinks(putLinks);

                    LOGGER.info("Обновление существующего автотеста с ID: {}", existing.getId());
                    autoTestsApi.updateAutoTest(putModel);
                    return Optional.of(existing);
                } else {
                    LOGGER.info("Создание нового автотеста с externalId: {}", model.getExternalId());
                    AutoTestModel result = autoTestsApi.createAutoTest(model);
                    LOGGER.info("Автотест создан с ID: {}", result.getId());
                    return Optional.of(result);
                }
            } catch (ApiException e) {
                LOGGER.error("Ошибка при создании/обновлении автотеста: {}", e.getMessage(), e);
                return Optional.empty();
            }
        }
    }

    /**
     * Внутренний класс-билдер для создания шагов автотеста.
     */
    public static class StepBuilder {
        private final AutoTestStepModel step = new AutoTestStepModel();

        public StepBuilder withTitle(String title) {
            step.setTitle(title);
            return this;
        }

        public StepBuilder withDescription(String description) {
            step.setDescription(description);
            return this;
        }

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

        private boolean hasCycle(AutoTestStepModel parent, AutoTestStepModel child) {
            if (child == null) return false;
            if (child == parent) return true;
            if (child.getSteps() == null) return false;
            for (AutoTestStepModel subStep : child.getSteps()) {
                if (hasCycle(parent, subStep)) return true;
            }
            return false;
        }

        public AutoTestStepModel build() {
            return step;
        }
    }
}
