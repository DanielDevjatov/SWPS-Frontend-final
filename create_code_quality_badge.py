import json
import os
import anybadge
from collections import Counter

def analyze_code_quality(file_path):
    with open(file_path, 'r') as file:
        findings = json.load(file)

    severity_counts = Counter(finding['severity'] for finding in findings)
    return severity_counts

def determine_quality_level(severity_counts):
    score = 100
    score -= severity_counts.get('critical', 0) * 20
    score -= severity_counts.get('major', 0) * 10
    score -= severity_counts.get('minor', 0)
    return max(score, 0)

def value_text(quality):
    value = "excellent"
    if quality == 0:
        value = "critical"
    elif quality <= 20:
        value = "bad"
    elif quality <= 40:
        value = "major"
    elif quality <= 60:
        value = "minor"
    elif quality <= 80:
        value = "good"
    elif quality < 100:
        value = "very good"

    return value

severity_counts = analyze_code_quality("gl-code-quality-report.json")

quality_level = determine_quality_level(severity_counts)

thresholds = {0: 'orangered',
              20: 'red',
              40: 'orange',
              60: 'yellow',
              80: 'yellow_green',
              100: 'green'}

badge = anybadge.Badge(
    label = 'Code Quality',
    value = value_text(quality_level),
    thresholds=thresholds,
    num_value_padding_chars=1
)

badge.write_badge('code_quality_badge.svg')
print(f"Quality badge created with level: {quality_level}")
