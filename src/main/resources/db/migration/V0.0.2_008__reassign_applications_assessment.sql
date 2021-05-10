INSERT INTO assessment_application
(id, createUser, createTime, assessment_id, application_id)
SELECT nextval('hibernate_sequence'), createUser, createTime, id, application_id
FROM assessment;

ALTER TABLE assessment_application
DROP COLUMN application_id;