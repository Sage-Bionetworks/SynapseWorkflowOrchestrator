#!/usr/bin/env cwl-runner
#
# parses a json file and returns a json object
#
cwlVersion: v1.0
class: ExpressionTool

requirements:
  - class: InlineJavascriptRequirement

inputs:
  - id: inputfile
    type: File
    inputBinding:
      loadContents: true

outputs:
  - id: content
    type: string?


expression: |
  ${
    return {"content":JSON.parse(inputs.inputfile.contents)};
  }
