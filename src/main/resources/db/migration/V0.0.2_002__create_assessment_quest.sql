    create table assessment_category (
       id int8 not null,
        createTime timestamp,
        createUser varchar(255),
        deleted boolean,
        updateTime timestamp,
        updateUser varchar(255),
        name varchar(255),
        category_order int4,
        questionnaire_id int8,
        primary key (id)
    )

    create table assessment_question (
       id int8 not null,
        createTime timestamp,
        createUser varchar(255),
        deleted boolean,
        updateTime timestamp,
        updateUser varchar(255),
        comment varchar(1000),
        name varchar(255) not null,
        question_order int4 not null,
        questionText varchar(500),
        tooltip varchar(1000),
        type varchar(255) not null,
        category_id int8 not null,
        primary key (id)
    )

    create table assessment_questionnaire (
       id int8 not null,
        createTime timestamp,
        createUser varchar(255),
        deleted boolean,
        updateTime timestamp,
        updateUser varchar(255),
        language_code varchar(255) not null,
        name varchar(255) not null,
        assessment_id int8 not null,
        questionnaire_id int8 not null,
        primary key (id)
    )

    create table assessment_singleoption (
       id int8 not null,
        createTime timestamp,
        createUser varchar(255),
        deleted boolean,
        updateTime timestamp,
        updateUser varchar(255),
        option varchar(500) not null,
        singleoption_order int4 not null,
        risk int4 not null,
        selected boolean not null,
        question_id int8 not null,
        primary key (id)
    )

    alter table if exists assessment_category 
       add constraint FKg7m8p00b2cw73bj67jh49ijq0 
       foreign key (questionnaire_id) 
       references assessment_questionnaire;
   
       alter table if exists assessment_question 
       add constraint FK7ya3lv02iu8jwfx0m2mo2mwfx 
       foreign key (category_id) 
       references assessment_category

    alter table if exists assessment_questionnaire 
       add constraint FK6pfuk1trqx58qfrr0e0nm77ms 
       foreign key (assessment_id) 
       references assessment

    alter table if exists assessment_questionnaire 
       add constraint FKs6mwnhrlyg8ae32ei95aigrmk 
       foreign key (questionnaire_id) 
       references questionnaire

    alter table if exists assessment_singleoption 
       add constraint FK7yuubk41hybcojhk3uin94bj1 
       foreign key (question_id) 
       references assessment_question

