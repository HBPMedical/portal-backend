UPDATE experiment
SET status = 'error'
WHERE
result LIKE '%text/plain+error%'
OR
result LIKE '%text/plain+warning%'
OR
result LIKE '%text/plain+user_error%';
