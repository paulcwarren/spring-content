#!/bin/bash -eu

function usage() {
  cat <<EOT
Usage: $(basename "$0") <options>
Options:
  -a  Builds the entire project
  -s  Builds the project identified by suffix (\`-s commons\` builds spring-content-commons)
EOT
}

function build() {
  
  if [[ -z "${1}" ]]; then
		BUILD_TYPE=dev ./mvnw clean compile
  elif [[ "${1}" == "all" ]]; then
		BUILD_TYPE=dev ./mvnw clean compile
  else
		BUILD_TYPE=dev ./mvnw -pl "spring-content-${1}" -am clean compile
  fi

}

function main() {
  local o

  while getopts as:h o; do
      case $o in
        a)
          build "all"
          ;;
        s)
          build "${OPTARG}"
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
