export interface AppInfo {
  build: {
    version: string
    artifact: string
    name:     string
    time:     string
    group:    string
  }
}

export interface Statistics {
  executionTime:    number
  mxCpuTime:        number
  statements:       number
  callDepth:        number
  objectsMax:       number
  memoryMax:        number
  objectCreations:  number
  mxMAlloc:         number
}

export interface ExecutionResult {
  errorCode:    number
  value:        string
  output:       string
  statistics:   Statistics
  errorMessage: string
  errorLine:    number
}
