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

with open('app.properties', 'r') as file:
    for line in file:
        if line.startswith('host'):
            instance_url = line.split('=', 1)[1].strip()
            break

username = sys.argv[1]
password = sys.argv[2]
base_url = instance_url
mc_auth = HTTPBasicAuth(username, password)

# List of project keys
project_keys = ["ZULU", "KILO", "NP"]  # Replace with actual project keys

for project_key in project_keys:
    print(f"Processing project key: {project_key}")

    query_url = f"{base_url}rest/api/2/search?jql=project = {project_key} AND issuetype = Test &maxResults=1"
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

            print(f"Project ID: {project_id}")
            print(f"Base URL: {base_url}")

            PostStatusURL = f"{base_url}rest/tests/1.0/testresultstatus"

            status_list = [
                {
                    "name": "Not Started",
                    "description": "Description for Not Started",
                    "color": "#B3BAC5"
                },
                {
                    "name": "WIP",
                    "description": "Description for WIP",
                    "color": "#FFAB00"
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

                response = requests.post(PostStatusURL, json=payload, headers=default_headers, auth=mc_auth)

                if response.status_code == 200:
                    print(f"Status {index} posted successfully.")
                else:
                    with open("error.txt", "a") as error_file:
                        error_file.write(
                            f"Error posting status {index} for project {project_key}: {response.status_code} - {response.text}\n"
                        )
                    print(f"Error posting status {index} for project {project_key}. Error details saved to error.txt")
                    sys.exit(1)

        else:
            print(f"No issues found for project key: {project_key}.")
            continue
    else:
        with open("error.txt", "a") as error_file:
            error_file.write(
                f"Error fetching issues for project {project_key}: {issues_response.status_code} - {issues_response.text}\n"
            )
        print(f"Error fetching issues for project {project_key}. Error details saved to error.txt")
        continue
