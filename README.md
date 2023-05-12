# Validation Service

### Curl examples

* Validate one request: curl --location 'localhost:8080/api/v1/validation' \
  --header 'Content-Type: application/json' \
  --data '{"id":"15887","customer_id":"528","load_amount":"$3318.47","time":"2000-01-01T00:00:00Z"}'
* Validate file: curl --location 'localhost:8080/api/v1/validation/process-file' \
  --form 'file=@"/Users/rayant/Downloads/input (1) (2).txt"'

## Notes

* For better history storing daily_count_accepted, daily_limit_accepted, weekly_limit_accepted are storing separately in db.
* Flyway is used for db migration, for test purpose db is cleared for each app start, to change this create table should be replaced with create if not exist.
* Added lock per clientId with Redis for sharing lock between different instances of app, it will prevent dirty read from db.
* For test purpose embedded Redis is used, it should be changed for dedicated server.
* 2 tests are added, one for file processing, and second for multi thread requests.

