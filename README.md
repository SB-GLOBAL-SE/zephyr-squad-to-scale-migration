# Zephyr Squad to Scale Migration Framework

[![Atlassian license](https://img.shields.io/badge/license-Apache%202.0-blue.svg?style=flat-square)](LICENSE) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](CONTRIBUTING.md)

This script executes a Migration From Zephyr Squad to Zephyr Scale on Jira DC/Server, with both apps on the same
instance, aiming customer that wishes to migrate from app to another before migrating to Jira to Cloud.
It uses Jira, Squad and Scale APIs to read and insert entities, executes some queries on Zephyr Scale Database
to fetch complementary data to help the migration and generates a CSV file with the mapping of the attachments to be
latter inserted.

## Disclaimer: Framework Status
The Zephyr Squad to Scale DC Migration Utility is provided as a framework to assist in the migration process between Zephyr Squad and Zephyr Scale. It is not a fully vetted, production-ready tool. The utility is designed to provide baseline functionality, but significant effort may be required to adapt it to your specific infrastructure and requirements.

## Usage

This script can be run in the following mode:

1. **Single Project Mode**: where you define the project key and the script will migrate all entities from that project
    ```bash
    java -jar zephyr-squad-to-scale-migration.jar <username> <password> <projectKey>
    ```


When the script finishes running, it will have migrated Squad Entities to Scale, copied all Attachments from Zephyr
Squad Entities to Zephyr Scale and generated a CSV file with the
attachments mapping. This file must be imported in the Zephyr Scale table `AO_4D28DD_ATTACHMENT` and to do so you can
use a command line, like so:

_Postgresql only_

```sql
psql
-U <username> -d <db_name> -c "\COPY \"AO_4D28DD_ATTACHMENT\" (\"FILE_NAME\",\"FILE_SIZE\",\"NAME\",\"PROJECT_ID\",\"USER_KEY\",\"TEMPORARY\",\"CREATED_ON\",\"MIME_TYPE\",\"TEST_CASE_ID\",\"STEP_ID\",\"TEST_RESULT_ID\") FROM /<CSV_FILE_NAME>.csv delimiter ',' CSV HEADER"
```

## Installation

### Prerequisites

**Java 17**

Ensure Java 17 is installed on your machine. Verify by running:

```bash
   java -version
```

**Java Installation**

<details>
    <summary>Linux</summary>

1. Update the package
   ```bash
   sudo apt-get update
   ```
2. Install Java 17
    ```bash
        sudo apt-get install openjdk-17-jdk
    ```
3. Check the installation
    ```bash
        java -version
    ```

</details>

<details>
    <summary>macOS</summary>

1. Install Homebrew
   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```
2. Install Java 17
    ```bash
        brew install openjdk@17
    ```

</details>

<details>
    <summary>Windows</summary>

1. Download the installer from the [Oracle](https://www.oracle.com/java/technologies/downloads/#java17) website.
2. Run the installer.
3. Set the `JAVA_HOME` environment variable:
    * Right-click on the `My Computer` icon on the desktop and select `Properties`.
    * Click on the `Advanced system settings` link.
    * Click on the `Environment Variables` button.
    * Under `System Variables`, click `New` and set the variable name as `JAVA_HOME` and the value as the path to the
      JDK installation directory.
    * Click `OK` to save.
4. Add the `JAVA_HOME\bin` directory to the `PATH` environment variable:
    * Find the `Path` variable in the `System Variables` section and click `Edit`.
    * Click `New` and add `%JAVA_HOME%\bin`.
    * Click `OK` to save.
5. Verify the installation:
    ```cmd
    java -version
    ```

</details>

### Script configuration

#### Properties setup

##### app.properties

| Fields                   | Used For                                                                                                    |
|--------------------------|-------------------------------------------------------------------------------------------------------------|
| host                     | Public address of Jira Instance                                                                             |
| batchSize                | How many Test Cases are processed per batch. Default is 100.                                                |
| attachmentsMappedCsvFile | The name of the resulting csv generated during the migration                                                |
| database                 | Name of the database used in the instance. Supported values are `postgresql`, `oracle`, `mssql` and `mysql` |
| attachmentsBaseFolder    | Where the attachments are located in the Instance Machine                                                   | 
| httpVersion              | Http version to be used in REST API Calls. Supported values `1.1`, `1`, `2`, `2.0`                          |

Example:

```
host=https://my-jira-instance-url.com
batchSize=100
attachmentsMappedCsvFile=AO_4D28DD_ATTACHMENT.csv
database=postgresql
attachmentsBaseFolder=/home/ubuntu/jira/data/attachments/
httpVersion=2
```

##### database.properties

| Fields                                       | Used for                                         |
|----------------------------------------------|--------------------------------------------------|
| <database type>.datasource.url               | Database url to access it                        |
| <database type>.datasource.driver.class.name | Database Driver. **You don't have to modify it** |
| <database type>.datasource.schema            | Schema holding Jira tables (Optional)            |
| <database type>.datasource.username          | database `username`                              |
| <database type>.datasource.password          | database `password`                              |

Example:

```
postgresql.datasource.url=jdbc:postgresql://localhost:5432/jira
postgresql.datasource.driver.class.name=org.postgresql.Driver
postgresql.datasource.schema=
postgresql.datasource.username=some_username
postgresql.datasource.password=some_password
```

#### Running in the right place

The Script must be run inside an Instance/Node running Jira. That is the case because it directly copies Attachments
from one directory to another. To do, you must move the JAR file alongside both `app.properties`
and `database.properties` (already configured) to the Jira host.

## Project build

The project is built using Maven and to run unit tests and build the `jar`. You can create the most up to date `jar` by the following command:

```bash
mvn clean package
```

## Documentation

### How it does it

The script uses Jira, Squad and Scale APIs to read and insert entities, executes some queries on Zephyr Scale Database.
APIs documentation:

- [Jira API](https://docs.atlassian.com/software/jira/docs/api/REST/9.11.0/)
- [Zephyr Squad API](https://zephyrsquadserver.docs.apiary.io/#reference)
- [Zephyr Scale API](https://support.smartbear.com/zephyr-scale-server/api-docs/v1/)

### What it does

This script is capable of migrating the Zephyr Squad entities, along with their attachments, to Zephyr Scale. The
following entities are supported:

- Test Cases and attachments 
- Test Steps and attachments
- Test Cycles
- Test Executions and attachments 
  
### Data Mapping

#### Test Case + Test Step Mappings

| **Squad Test Case Field**| **Scale Test Case Field**       | **Description**                                                                                   |
|--------------------------|---------------------------------|---------------------------------------------------------------------------------------------------|
| Summary                  | Name                            | The Squad’s test case Summary field goes to the Name field inside the Scale test case.            |
| Description              | Objective                       | The Squad’s test case Description field goes to the  Objective field in Scale’s test case.        |
| Labels                   | Labels                          | The data from labels of Squad’s test case migrates to Scale’s labels.                             |
| Reporter                 | Owner                           | The system migrates Squad's Reporter field data to Scale’s test case Owner field.                 |
| Issue links              | Test case → coverage            | The system connects issue link to Jira issue inside the traceability section of the test case.    |
| Test Step                | Step                            | The system migrates Squad Test Step, to Scale’s Step. Html values will migrate                    |
| Test Data                | Test Data                       | The system migrates Squad Step Test Data, to Scales Test Data . Html values will migrate          |
| Test Result              | Expected Result                 | The system migrates Squad Step Test Result, to Scale’s Expected Result. Html values will migrate. |
| Status                   | Custom Field - squadStatus      | The system creates a custom field for test case in Scale SquadStatus value                        |
| Priority                 | Custom Field - squadPriority    | The system creates a custom field for test case in Scale SquadPriority value                      |
| Component                | Custom Field - components       | The system creates a custom field for test case in Scale components value                         |
| Attachments              | Attachments                     | Attachments attached at the test cases, and test steps will migrate.                              |


#### Test Execution Mappings

| **Squad Test Execution Field**| **Scale Test Execution Field**| **Description**                                                                          |
|-------------------------------|-------------------------------|------------------------------------------------------------------------------------------|
| Execution Value               | Execution Value               | The system migrates the execution value from the Squad. Like “Pass”, “Failed”, “WIP”     |
| Comment                       | Comment                       | Test Execution comments are migrated.                                                    |
|                               | Executed by                   | Jira user that executed the migration script                                             |
| Executed On                   | Custom Field - executedOn     | The system creates a custom field for test execution in Scale executedOn value           |
| Assignee                      | Custom Field - assignedTo     | The system creates a custom field for test execution in Scale assignedTo value           |
| Version                       | Custom Field - squadVersion   | The system creates a custom field for test execution in Scale squadVersion value         |
| Test Cycle                    | Custom Field - squadCycleName | The system creates a custom field for test execution in Scale squadCycleName value       |
| Folder                        | Custom Field - folderName     | The system creates a custom field for test execution in Scale folderName value           |
| Attachments                   | Attachments                   | Attachments attached at the test execution entity will migrate. NOT test step executions |
 
#### Test Cycle Mappings

| **Squad Test Cycle Field**| **Scale Test Cycle Field**| **Description**                                      |
|---------------------------|---------------------------|------------------------------------------------------|
| Test Cycle                | Name                      | The system migrates the test cycle name.             |
| Test Cases                |Test Cases                 | The system migrates the test cases within that cycle.|



### What it doesn't do

- **Fetch data only through public APIs**: Some of the data needed during the migration is not accessible through the
  public APIs
  and must be fetched directly from the database
- **Run remotely**: The script directly copies attachments from Squad directories to Scale directory, so it need access
  to both.
- **Check if a project was already migrated**: The script does not check if a project was already migrated, so it may
  duplicate data if run multiple times on the same project
- **Clean Zephyr Scale data**: Currently, there is no easy way to clean Zephyr Scale after an unsuccessful/undesirable
  migration. It must be done manually through the UI or Database.
- **Automated attachments import**: The script generates a CSV file with the attachments mapping, but it does not import
  it
  automatically. It must be done manually through a third-party tool or command line.
- **Migrate Custom Statuses or Priorities**: If you use priorities, test case statuses, or test execution statuses beyond the default values, then you must migrate those prior to running the utility. Start-up.py is an example for test execution statuses.
**Migrate Test Execution Step Information**: The framework does NOT migrate test execution step statuses, execution step defects, execution step attachments, and execution step comments.


## Common Errors and Resolutions

### General Guidance
You must parse the `app.log` file that gets generated to understand the failure, its cause, and when it occurred. The `app.log` contains logs of most actions, including successes and failures. Look specifically for the following messages:

- **"Failed to execute the migration"**
- **"Migration Completed"**

### Error Messages and Resolutions

#### Error: `"errorMessages":["Failed to execute the migration: null"]`
- **Cause:** This error typically occurs at the beginning of the process due to misconfigured `app.properties` or `database.properties` files.
- **Resolution:** Check the configuration files (`app.properties` and `database.properties`) against the examples provided above.


#### Error: `"errorMessages":["Failed to execute the migration: app.properties (No such file or directory)"]`
- **Cause:** The `app-migration-zephyr-1.0.0.jar` file is not in the same directory as either `app.properties` or `database.properties`.
- **Resolution:** Ensure that `app.properties`, `database.properties`, and the `.jar` file are all in the same directory.


#### Error: `"errorMessages":["The value <any value> was not found for field status on project <Jira Project Key>."]`
- **Cause:** This indicates that a non-standard test execution status has not been migrated.
- **Resolution:** Migrate the non-standard test execution statuses as described under **Migrate Custom Statuses**.

#### Error: `"errorMessages":["HTTP/1.1 header parser received no bytes"]`
- **Cause:** This occurs when the `host` parameter in `app.properties` is incorrectly configured.
- **Resolution:** Change the `host` parameter in `app.properties` to `http://localhost:8080` instead of host=`<https://your-jira-instance.atlassian.net>`


## Contributions

Contributions to Zephyr Squad to Scale Migration script are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for
details.

## License

Copyright (c) 2024 Atlassian US., Inc.
Apache 2.0 licensed, see [LICENSE](LICENSE) file.

[![With â¤ï¸ from Atlassian](https://raw.githubusercontent.com/atlassian-internal/oss-assets/master/banner-with-thanks.png)](https://www.atlassian.com)



