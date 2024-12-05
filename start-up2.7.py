import requests
from requests.auth import HTTPBasicAuth
import json
import os
import sys

# Clear error.txt if it exists from a previous run
if os.path.exists("error.txt"):
    os.remove("error.txt")

if len(sys.argv) != 3:
    print("Usage: python start-up2.7.py <username> <password>")
    sys.exit(1)

# Read instance URL from app.properties
instance_url = None
with open('app.properties', 'r') as file:
    for line in file:
        if line.startswith('host'):
            instance_url = line.split('=', 1)[1].strip()
            break

if not instance_url:
    print("Error: Host URL not found in app.properties.")
    sys.exit(1)

username = sys.argv[1]
password = sys.argv[2]
base_url = instance_url
mc_auth = HTTPBasicAuth(username, password)

# List of project keys
project_keys = ["PROJ1", "PROJ2", "PROJ3"]  # Replace with actual project keys

for project_key in project_keys:
    print("Processing project key: {}".format(project_key))

    query_url = "{}rest/api/2/search?jql=project = {} AND issuetype = Test &maxResults=1".format(base_url, project_key)
    default_headers = {
        "Accept": "application/json",
        "Content-Type": "application/json"
    }

    issues_response = requests.get(query_url, headers=default_headers, auth=mc_auth)

    if issues_response.status_code == 200:
        response_data = issues_response.json()

        # Check if there are issues in the response
        if "issues" in response_data and response_data["issues"]:
            first_issue = response_data["issues"][0]

            issue_id = first_issue["id"]
            project_id = first_issue["fields"]["project"]["id"]

            print("Project ID: {}".format(project_id))
            print("Base URL: {}".format(base_url))

            PostStatusURL = "{}rest/tests/1.0/testresultstatus".format(base_url)

            status_list = [
                {
                    "name": "Not Started",
                    "description": "Description for Not Started",
                    "color": "#B3BAC5"
                },
                {
                    "name": "Descoped",
                    "description": "Description for Descoped",
                    "color": "#091E42"
                },
                {
                    "name": "Deferred",
                    "description": "Description for Deferred",
                    "color": "#0065FF"
                },
                {
                    "name": "Retest",
                    "description": "Description for Retest",
                    "color": "#79E2F2"
                }
            ]

            os.environ['MIGRATION_ID'] = str(project_id)
            os.environ['MIGRATION_BASE_URL'] = base_url

            # Iterate through the status_list and send request to create new status.
            for index, status_details in enumerate(status_list):
                payload = {
                    "projectId": int(project_id),
                    "name": status_details["name"],
                    "description": status_details["description"],
                    "color": status_details["color"],
                    "index": index,
                    "items": []
                }

                response = requests.post(PostStatusURL, data=json.dumps(payload), headers=default_headers, auth=mc_auth)

                if response.status_code == 200:
                    print("Status {} posted successfully.".format(index))
                else:
                    with open("error.txt", "a") as error_file:
                        error_file.write(
                            "Error posting status {} for project {}: {} - {}\n".format(
                                index, project_key, response.status_code, response.text
                            )
                        )
                    print("Error posting status {} for project {}. Error details saved to error.txt".format(index, project_key))
                    sys.exit(1)

        else:
            print("No issues found for project key: {}.".format(project_key))
            continue
    else:
        with open("error.txt", "a") as error_file:
            error_file.write(
                "Error fetching issues for project {}: {} - {}\n".format(project_key, issues_response.status_code, issues_response.text)
            )
        print("Error fetching issues for project {}. Error details saved to error.txt".format(project_key))
        continue
