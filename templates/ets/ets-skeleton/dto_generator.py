import os
import json
from glob import glob
from pathlib import Path
import shutil
import subprocess
import sys
import fileinput


def main():
    print("Starting the DTO Generator")

    local_generated_dto_path = "ets-server/src/main/java/com/sailpoint/ets/domain/trigger/schemaGeneratedDto/"
    json_schema_path = "ets-server/src/main/resources/triggers/json_schemas"
    json_load_dir = "ets-server/src/main/resources/triggers/jsonSchema/*.json"

    print("Removing stale files")
    remove_files(json_schema_path)
    remove_files(local_generated_dto_path)

    print("Loading schema files")
    file_contents = load_json_schema_files(json_load_dir)

    print("Generating helper files")
    for file_content in file_contents:
        input_schema = file_content.get("inputSchema")
        output_schema = file_content.get("outputSchema")

        if input_schema != None:
            write_schema_to_json_file(input_schema, "input_schema", file_content.get("id"), json_schema_path)

        if output_schema != None:
            write_schema_to_json_file(output_schema, "output_schema", file_content.get("id"), json_schema_path)

    print("Generating DTOS at ets-server/src/main/java/com/sailpoint/ets/domain/trigger/schemaGeneratedDto/")
    generate_dto_classes(json_schema_path, local_generated_dto_path)

    print("Modifying package and json ignore annotations in generated dtos")
    modify_dto_classes(local_generated_dto_path)

    print("Removing helper files")
    remove_files(json_schema_path)

    print("DTO Generator complete")


def getListOfFiles(directory_name):
    list_of_file = os.listdir(directory_name)
    all_files = list()

    for entry in list_of_file:
        full_path = os.path.join(directory_name, entry)

        if os.path.isdir(full_path):
            all_files = all_files + getListOfFiles(full_path)
        else:
            all_files.append(full_path)

    return all_files


def modify_dto_classes(local_generated_dto_path):
    package_string = " com.sailpoint.ets.domain.trigger.schemaGeneratedDto."

    for file_name in getListOfFiles(local_generated_dto_path):
        print("\tModifying " + file_name)
        line_prepender(file_name, "/*\n* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.\n */")
        line_replacer_enhanced(file_name, "package ", package_string)

        #Depending on different version, one of these two import locations will be used. But not both
        line_replacer(file_name, "import javax.annotation.processing.Generated;",
                      "import com.fasterxml.jackson.annotation.JsonIgnoreProperties;\n")
        line_replacer(file_name, "import javax.annotation.Generated;",
                      "import com.fasterxml.jackson.annotation.JsonIgnoreProperties;\n")

        line_replacer(file_name, '@Generated("jsonschema2pojo")', "@JsonIgnoreProperties(ignoreUnknown = true)\n")
        line_replacer(file_name, '@JsonInclude(JsonInclude.Include.NON_NULL)',
                      "@JsonInclude(JsonInclude.Include.ALWAYS)\n")


def line_prepender(filename, line):
    with open(filename, 'r+') as f:
        content = f.read()
        f.seek(0, 0)
        f.write(line.rstrip('\r\n') + '\n' + content)


def line_replacer(filename, target_line, replace_contents):
    for line in fileinput.input([filename], inplace=True):
        if line.strip().startswith(target_line):
            line = replace_contents
        sys.stdout.write(line)


def line_replacer_enhanced(filename, target_line, replace_contents):
    for line in fileinput.input([filename], inplace=True):
        if line.strip().startswith(target_line):
            line = line.replace(" ", (replace_contents))
        sys.stdout.write(line)


def generate_dto_classes(json_schema_path, generated_dto_path):
    print(subprocess.check_output(['jsonschema2pojo', '--source', json_schema_path, '--target', generated_dto_path]))


def write_schema_to_json_file(json_data, suffix, id, json_schema_path):
    Path(json_schema_path + "/" + id).mkdir(parents=True, exist_ok=True)
    file_name = json_schema_path + "/" + id + "/" + suffix + ".json"
    with open(file_name, 'w') as f:
        json.dump(json_data, f)


def load_json_schema_files(json_load_dir):
    data = []
    for file_name in glob(json_load_dir):
        print("\tLoading: " + str(file_name))
        with open(file_name) as f:
            data.append(json.load(f))

    return data


def remove_files(path):
    dirpath = Path(path)
    if dirpath.exists() and dirpath.is_dir():
        shutil.rmtree(dirpath)


if __name__ == "__main__":
    main()
