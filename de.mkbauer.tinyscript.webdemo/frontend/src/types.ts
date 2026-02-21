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
