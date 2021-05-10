create table assessment_application (
    id int8 not null,
    createTime timestamp,
    createUser varchar(255),
    deleted boolean not null default false,
    updateTime timestamp,
    updateUser varchar(255),
    application_id int8,
    assessment_id int8,
    primary key (id)
);

alter table if exists assessment_application
    add constraint assessment_application_to_assessment_FK foreign key (assessment_id) references assessment;