CREATE TABLE "user" (
                        username CHARACTER VARYING PRIMARY KEY,
                        subject_id CHARACTER VARYING,
                        fullname CHARACTER VARYING,
                        email CHARACTER VARYING,
                        agree_nda BOOLEAN DEFAULT FALSE
);

CREATE TABLE "experiment" (
                              uuid UUID PRIMARY KEY,
                              name TEXT,
                              created_by_username CHARACTER VARYING REFERENCES "user"(username),
                              status TEXT CHECK (status IN ('error', 'pending', 'success')),
                              result TEXT,
                              finished TIMESTAMP WITHOUT TIME ZONE,
                              algorithm TEXT,
                              algorithm_id TEXT,
                              created TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              updated TIMESTAMP WITHOUT TIME ZONE,
                              shared BOOLEAN DEFAULT FALSE,
                              viewed BOOLEAN DEFAULT FALSE
);
