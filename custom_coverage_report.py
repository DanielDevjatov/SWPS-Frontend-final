# Summary (FinalFinal): Added code to *.\r\n# Purpose: document changes and explain behavior.\r\nimport os
import json
import re
import sys

is_project_file_regex = re.compile(f"(.*/)*/deer-prototype/{sys.argv[1]}/src")

file_path = "coverage/coverage-summary.json"
with open(file_path, 'r') as file:
    report = json.load(file)

    total_stmts = 0
    total_branch = 0
    total_func = 0
    total_lines = 0

    covered_stmts = 0
    covered_branch = 0
    covered_func = 0
    covered_lines = 0

    for entry in report:
        if re.search(is_project_file_regex, entry) != None:
            total_stmts += report[entry]["statements"]["total"]
            total_branch += report[entry]["branches"]["total"]
            total_func += report[entry]["functions"]["total"]
            total_lines += report[entry]["lines"]["total"]

            covered_stmts += report[entry]["statements"]["covered"]
            covered_branch += report[entry]["branches"]["covered"]
            covered_func += report[entry]["functions"]["covered"]
            covered_lines += report[entry]["lines"]["covered"]

    try:
        print("=============================== Coverage summary ===============================")
        print(f"Statements   : {((covered_stmts / total_stmts ) * 100):3.0f}% ( {covered_stmts}/{total_stmts} )")
        print(f"Branches     : {((covered_branch / total_branch ) * 100):3.0f}% ( {covered_branch}/{total_branch} )")
        print(f"Functions    : {((covered_func / total_func ) * 100):3.0f}% ( {covered_func}/{total_func} )")
        print(f"Lines        : {((covered_lines / total_lines ) * 100):3.0f}% ( {covered_lines}/{total_lines} )")
        print("================================================================================")
    except:
        print("Couldn't gather coverage data.")

