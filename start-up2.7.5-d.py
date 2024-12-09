import requests
from requests.auth import HTTPBasicAuth
import json
import os
import sys

# Clear error.txt if it exists
if os.path.exists("error.txt"):
    os.remove("error.txt")

if len(sys.argv) != 4:
    print("Usage: python start-up2.7.py <username> <password> <projectKey1,projectKey2,...>")
    sys.exit(1)

# Reading instance URL
try:
    with open('app.properties', 'r') as file:
        instance_url = next((line.split('=', 1)[1].strip() for line in file if line.startswith('host')), None)
        if not instance_url:
            raise ValueError("Missing 'host' in app.properties")
except (IOError, ValueError) as e:
    print("Error reading app.properties:", e)
    sys.exit(1)

username = sys.argv[1]
password = sys.argv[2]
project_keys = sys.argv[3].split(',')
base_url = instance_url
mc_auth = HTTPBasicAuth(username, password)

# Default headers
default_headers = {
    "Accept": "application/json",
    "Content-Type": "application/json"
}

for project_key in project_keys:
    print("Processing project key:", project_key.strip())

    query_url = "{}rest/api/2/search?jql=project = {} AND issuetype = Test &maxResults=1".format(
        base_url, project_key.strip())
    
    try:
        issues_response = requests.get(query_url, headers=default_headers, auth=mc_auth)
        if issues_response.status_code == 200:
            response_data = issues_response.json()
            if "issues" in response_data and response_data["issues"]:
                first_issue = response_data["issues"][0]
                project_id = first_issue["fields"]["project"]["id"]

                print("Project ID:", project_id)
                print("Base URL:", base_url)

                PostStatusURL = "{}rest/tests/1.0/testresultstatus".format(base_url)
                status_list = [
                    {"name": "Not Started", "description": "Description for Not Started", "color": "#B3BAC5"},
                    {"name": "WIP", "description": "Description for WIP", "color": "#FFAB00"},
                    {"name": "Descoped", "description": "Description for Descoped", "color": "#091E42"},
                    {"name": "Deferred", "description": "Description for Deferred", "color": "#0065FF"},
                    {"name": "Retest", "description": "Description for Retest", "color": "#79E2F2"}
                ]

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
                        print("Status {} posted successfully.".format(index))
                    else:
                        with open("error.txt", "a") as error_file:
                            error_file.write("Error posting status {} for project {}: {} - {}\n".format(
                                index, project_key.strip(), response.status_code, response.text))
                        print("Error posting status {} for project {}. See error.txt".format(index, project_key.strip()))
                        sys.exit(1)
            else:
                print("No issues found for project key:", project_key.strip())
        else:
            raise Exception("Error fetching issues: {} - {}".format(
                issues_response.status_code, issues_response.text))
    except Exception as e:
        with open("error.txt", "a") as error_file:
            error_file.write("Error processing project {}: {}\n".format(project_key.strip(), str(e)))
        print("Error processing project {}. See error.txt".format(project_key.strip()))
        continue
