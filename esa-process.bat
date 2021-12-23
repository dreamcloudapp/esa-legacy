if not exist %2 mkdir %2

rem call ./esa.bat --preprocess %1 %2\preprocessed.xml.bz2 %2\titles.xml.bz2 --title-exclusion-regex "^[^:]+:[^ ].+$" "^(january)|(february)|(march)|(april)|(may)|(june)|(july)|(august)|(september)|(november)|(december)] .+" "[0-9]{1,4}(s)?( bc)?" "disambiguation" "wikiproject" "wikipedia"

 call ./esa.bat --count-links-and-terms %2\preprocessed.xml.bz2 %2\titles.xml.bz2 %2\annotated.xml.bz2 --min-incoming-links 5 --min-outgoing-links 5 --min-terms 100

rem call ./esa.bat --repeat-content %2\terms.xml.bz2 %2\repeated.xml.bz2 --repeat-title 4 --repeat-link 2

rem call ./esa.bat --write-rare-words %2\annotated.xml.bz2 %2\rare-words.txt 3 --filter classic ascii lower singular --stopwords en-default --min-word-length 3