#!/bin/bash -eu

function usage() {
  cat <<EOT
Usage: $(basename "$0") <options>
Options:
  -s  Test project identified by suffix (\`-s .\` test all) 
EOT
}

function test() {
  
  if [[ -z "${1}" ]]; then
		BUILD_TYPE=dev mvn clean test     
  elif [[ "${1}" == "all" ]]; then
		BUILD_TYPE=dev mvn clean test     
  else
		BUILD_TYPE=dev mvn -pl "spring-content-${1}" -am clean test
  fi

}

function main() {
  local o

  while getopts a:s:h: o; do
      case $o in
        a)
          test
          ;;
        s)
          test "${OPTARG}"
          ;;
        *)
          echo "Invalid flag: ${o}"
          usage
          exit 1
      esac
    done

   shift $((OPTIND-1))
}

main "$@"
