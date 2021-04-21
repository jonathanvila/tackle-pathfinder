#!/bin/sh
set -e

# Usage : check_api.sh api_ip keycloak_api , both arguments optional defaulted to $(minikube ip)
# ./check_api.sh localhost:8085 localhost:8180
# ./check_api.sh

api_ip=${1:-$(minikube ip)}
keycloak_ip=${2:-$api_ip}

echo "Using API IP: $api_ip   &   Keycloak IP: $keycloak_ip"
echo
echo '>>> Obtaining the keycloack token for the operations'
access_token=$(curl -X POST "http://$keycloak_ip/auth/realms/quarkus/protocol/openid-connect/token" \
            --user backend-service:secret \
            -H 'content-type: application/x-www-form-urlencoded' \
            -d 'username=alice&password=alice&grant_type=password' | jq --raw-output '.access_token')
echo 
echo
echo '>>> Given a NOT assessed app When Get Assessments ThenResult Empty body with 200 http code'
req_not_existing_app=$(curl -X GET "http://$api_ip/pathfinder/assessments?applicationId=100" -H 'Accept: application/json' \
            -H "Authorization: Bearer $access_token" -s -w "%{http_code}")
test "$req_not_existing_app" = "[]200"

echo 
echo
echo '>>> Given a NOT assessed app When Create Assessment ThenResult AssessmentHeader body with 201 http code'
curl "http://$api_ip/pathfinder/assessments" \
            -H 'Content-Type: application/json' -H 'Accept: application/json' -H "Authorization: Bearer $access_token" \
            -d '{ "applicationId": 100 }' -w "%{http_code}" |
    grep '"applicationId":100,"status":"STARTED"}201'

echo 
echo
echo '>>>Given an assessed app When Get AssessmentHeader ThenResult AssessmentHeader body with 200 http code'
req_get_assessment=$(curl -X GET "http://$api_ip/pathfinder/assessments?applicationId=100" -H 'Accept: application/json' \
            -H "Authorization: Bearer $access_token" -s -w "%{http_code}")
echo $req_get_assessment | grep '"applicationId":100,"status":"STARTED"}]200' 
req_get_assessment=$(echo $req_get_assessment | sed 's/]200/]/g')

echo 
echo
echo '>>> Given an assessed app When Get Assessment ThenResult Assessment body with 200 http code'
assessmentId=$(echo $req_get_assessment | jq '.[0].id')
assessment_json=$(curl -X GET "http://$api_ip/pathfinder/assessments/$assessmentId" -H 'Accept: application/json' \
            -H "Authorization: Bearer $access_token" -s)
echo $assessment_json | grep '"order":5,"option":"Application containerisation not attempted as yet"'
categoryid=$(echo $assessment_json | jq '.questionnaire.categories[0].id')
questionid=$(echo $assessment_json | jq '.questionnaire.categories[0].questions[0].id')
optionid=$(echo $assessment_json | jq '.questionnaire.categories[0].questions[0].options[0].id')

echo
echo
echo '>>> Checking that default values on recently created assessment are present'
test "$(echo $assessment_json | jq '.stakeholders | length ')" = "0"
test "$(echo $assessment_json | jq '.stakeholderGroups | length')" = "0"
test "$(echo $assessment_json | jq ".questionnaire.categories[] | select(.id == $categoryid) | .comment // \"empty\" ")" = '"empty"'
test "$(echo $assessment_json | jq ".questionnaire.categories[] | select(.id == $categoryid) | .questions[] | select(.id == $questionid) | .options[] | select(.id == $optionid) | .checked")" = "false"


echo 
echo
echo '>>> Given a NOT assessed app When Get Assessment ThenResult 404 http code'
assessmentIdNotFound="1000"
req_get_not_assessed=$(curl -X GET "http://$api_ip/pathfinder/assessments/$assessmentIdNotFound" -H 'Accept: application/json' \
            -H "Authorization: Bearer $access_token" -w "%{http_code}")
test "$req_get_not_assessed" = "404"

echo
echo
echo '>>> Given an assessment, update few values and check return value is 200'
curl -X PATCH "http://$api_ip/pathfinder/assessments/$assessmentId" -H 'Accept: application/json' \
            -H "Authorization: Bearer $access_token" -w "%{http_code}" \
            -H 'Content-Type: application/json' \
            -d "{ \"id\": $assessmentId,\"status\": \"STARTED\",\"stakeholders\": [77,44],\"stakeholderGroups\": [333,222,111],\"questionnaire\": {\"categories\": [{\"id\": $categoryid,\"comment\" : \"This is a test comment\",\"questions\": [{\"id\": $questionid,\"options\": [{\"id\": $optionid,\"checked\": true}]}]}]}}" \
             | grep '"applicationId":100,"status":"STARTED"}200'

echo
echo
echo '>>> Give an updated assessment, check values have been successfully stored'
assessment_updated_json=$(curl -X GET "http://$api_ip/pathfinder/assessments/$assessmentId" -H 'Accept: application/json' \
            -H "Authorization: Bearer $access_token" -s)
test "$(echo $assessment_updated_json | jq '.stakeholders | length')" = "2"
test "$(echo $assessment_updated_json | jq '.stakeholderGroups | length')" = "3"
test "$(echo $assessment_updated_json | jq ".questionnaire.categories[] | select(.id == $categoryid) | .comment // \"empty\"")" = '"This is a test comment"'
test "$(echo $assessment_updated_json | jq ".questionnaire.categories[] | select(.id == $categoryid) | .questions[] | select(.id == $questionid) | .options[] | select(.id == $optionid) | .checked")" = "true"

echo
echo
echo '>>> Given a created assessment, delete it and expect 204 as return code'
req_delete_assessment=$(curl -X DELETE "http://$api_ip/pathfinder/assessments/$assessmentId" -H 'Accept: application/json' \
            -H "Authorization: Bearer $access_token" -w "%{http_code}")
test "$req_delete_assessment" = "204"

echo
echo
echo '>>> Given a deleted assessment, request it and receive 404 as return code'
req_get_delete_assessment=$(curl -X GET "http://$api_ip/pathfinder/assessments/$assessmentId" -H 'Accept: application/json' \
            -H "Authorization: Bearer $access_token" -w "%{http_code}")
test "$req_get_delete_assessment" = "404"

echo
echo
echo '>>> Given a deleted assessment, request assessments by application id and receive empty list'
req_find_delete_assessment=$(curl -X GET "http://$api_ip/pathfinder/assessments?applicationId=100" -H 'Accept: application/json' \
            -H "Authorization: Bearer $access_token" -w "%{http_code}")
test "$req_find_delete_assessment" = "[]200"


    
echo " +++++ API CHECK SUCCESSFUL ++++++"
