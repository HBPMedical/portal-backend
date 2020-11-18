ALTER TABLE experiment
DROP COLUMN haserror,
DROP COLUMN hasservererror,
DROP COLUMN validations,
DROP COLUMN model_slug;

ALTER TABLE experiment
RENAME algorithms TO algorithmDetails;
ALTER TABLE experiment
RENAME createdby_username TO created_by_username;
ALTER TABLE experiment
RENAME workflowhistoryid TO workflow_history_id;
ALTER TABLE experiment
RENAME resultsviewed TO viewed;
ALTER TABLE experiment
RENAME workflowstatus TO status;

ALTER TABLE experiment
ADD COLUMN updated timestamp without time zone;

ALTER TABLE experiment
ADD COLUMN algorithm text;

ALTER TABLE "user"
DROP COLUMN birthday,
DROP COLUMN city,
DROP COLUMN country,
DROP COLUMN firstname,
DROP COLUMN gender,
DROP COLUMN isactive,
DROP COLUMN lastname,
DROP COLUMN password,
DROP COLUMN phone,
DROP COLUMN picture,
DROP COLUMN team,
DROP COLUMN web;

DROP TABLE "config_title", "config_yaxisvariables";
DROP TABLE "dataset_variable", "dataset_grouping", "dataset_data", "dataset_header";
DROP TABLE "query_variable", "query_grouping", "query_filter", "query_covariable";
DROP TABLE "article_tag", "tag";
DROP TABLE "variable_value";
DROP TABLE "query_training_datasets", "query_validation_datasets", "query_testing_datasets";
DROP TABLE "variable", "value";
DROP TABLE "group_group", "group";
DROP TABLE "model";
DROP TABLE "query";
DROP TABLE "dataset";
DROP TABLE "config";
DROP TABLE "vote", "app";
DROP TABLE "user_roles", "user_languages";