CREATE OR REPLACE FUNCTION ISJSON(p_json text)
    RETURNS boolean
AS
$$
BEGIN
    RETURN (p_json::json is not null);
EXCEPTION
    WHEN others THEN
        RETURN false;
END;
$$
language plpgsql
immutable;
UPDATE experiment SET result = result::json #>>'{0,result}' WHERE (algorithm::json->>'type') <> 'workflow' and ISJSON(result);
UPDATE experiment SET result = '', status = 'error' WHERE NOT ISJSON(result);
