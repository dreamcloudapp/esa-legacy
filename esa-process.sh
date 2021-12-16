mkdir -p $2

./esa.bat --preprocess $1 $2/preprocessed.xml.bz2 $2/titles.xml.bz2 --title-exclusion-regex "^(?!Category)[^:]+:[^ ].+$" "^(january)|(february)|(march)|(april)|(may)|(june)|(july)|(august)|(september)|(november)|(december)] .+" "[0-9]{1,4}(s)?( bc)?" disambiguation wikiproject wikipedia

./esa.bat --count-links $2/preprocessed.xml.bz2 $2/titles.xml.bz2 $2/links.xml.bz2

./esa.bat --count-terms $2/links.xml.bz2 $2/terms.xml.bz2 --filter classic ascii lower singular stemmer --stemmer-depth 3 --stopwords en-default

# ./esa.bat --repeat-content $2/terms.xml.bz2 $2/repeated.xml.bz2 --repeat-title 4 --repeat-link 2

./esa.bat --write-rare-words $2/terms.xml.bz2 $2/rare-words.txt 3 --filter classic ascii lower singular --stopwords en-default --min-word-length 3