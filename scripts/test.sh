#!/bin/bash -eu

function usage() {
  cat <<EOT
Usage: $(basename "$0") <options>
Options:
  -a  Test entire project
  -s  Test project identified by suffix (\`-s commons\` tests spring-content-commons)
EOT
}

function test() {
  
  if [[ -z "${1}" ]]; then
		BUILD_TYPE=dev mvn clean install     
  elif [[ "${1}" == "all" ]]; then
		BUILD_TYPE=dev mvn clean install     
  else
		BUILD_TYPE=dev mvn -pl "spring-content-${1}" -am clean install
  fi

}

function main() {
  local o

  while getopts as:h o; do
      case $o in
        a)
          test "all"
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
