# Wikipedia-based Explicit Semantic Analysis

## What is this?

Semantic analysis is a way to extract meaning from a written text.
The written text may be a single word, a couple of words, a sentence, a paragraph or a whole book.

Explicit Semantic Analysis is a way to derive meaning based on Wikipedia.
The text is transformed into a vector of Wikipedia articles.
The vectors of two different texts can then be compared to assess the semantic similarity of those texts.

This implementation was written by Philip van Oosten.
It takes advantage of the mature Lucene project.

## License

This software is provided under the terms of the AGPLv3 license.
If this software seems helpful to you, but you dislike the licensing, don't let it get in your way and contact the author.
We can work something out.

## Usage

ESA can be used as a library. You will need to make some changes to the source code to use ESA and to tweak it.

To learn how to work with it, I recommend trying a language with a small Wikipedia dump, other than English.
The English wikipedia dump is very large and each step in the process of setting up ESA takes several hours to complete.
A language with a smaller Wikipedia dump may not work as good as English, because there is just less data, but you will
get up and running much faster.

## Quick start

### 1) Install Java

If you haven't set up Java on your computer, you'll need that.

**On Mac**
Using Homebrew,
`brew install java`
then install Maven:
`brew install maven`
Also, you'll need the Java Development Kit:
[https://www.oracle.com/java/technologies/downloads/#jdk17-mac](https://www.oracle.com/java/technologies/downloads/#jdk17-mac)
Use the `Arm 64 DMG Installer` or `x64 DMG Installer`, depending on your system.

### 2) Install packages using Maven

In the home folder of the repo, run:
`mvn package`
Remember on any code changes or branch switches, you'll need to re-run this command.

### 3) Make folders for Wikipedia data

In the home folder of the repo:
`mkdir index` (for the Lucene index)
`mkdir enwiki` (to store your English Wikipedia download. You could use a different folder name and then different commands if you like).

### 4) Download a Wikipedia dump

A list of all available database dumps is available here: <https://dumps.wikimedia.org/backup-index-bydb.html>.

Click on a Wikipedia version, it's good to start with a Simple Wikipedia version if you're using English:

![image](https://user-images.githubusercontent.com/14936307/145384562-2431a7d5-bd36-454c-8779-241414e1f5a9.png)

On the next page, choose a download which contains all current articles without history, such as this:

![image](https://user-images.githubusercontent.com/14936307/145387013-26238b20-8be5-4803-9775-281231ac1c45.png)

**Direct links to older versions:**
For English ([enwiki](https://dumps.wikimedia.org/enwiki/20160801/enwiki-20160801-pages-articles-multistream.xml.bz2)), the download size is 13 GB at the time of writing, for Dutch ([nlwiki](https://dumps.wikimedia.org/nlwiki/20160801/nlwiki-20160801-pages-articles-multistream.xml.bz2)) it is 1.3 GB.
Note that Wikipedia is constantly updated, so old dumps may not contain new concepts that could be interesting for your application.

The Wikipedia article dump consists of a multi-stream BZipped xml file.
That means that a straightforward way to read the bzip stream ends somewhere near the beginning of the file.
You need to read the whole dump, not just the beginning.
This implementation takes care of that.

### 5) Put the dump in the folder

Take your downloaded Wikipedia database dump (a zipped file in .bz2 format, e.g. `simplewiki-20211201-pages-articles-multistream.xml.bz2`), and put it in the folder you just made, e.g. `enwiki`.

### 6) Build the index

#### Pre-processing

This can take some time, depending on your system:

- Make the `esa` file executable: `chmod +x esa`
- Run the script:

<<<<<<< HEAD
1. Resolve templates: `./esa --resolve-templates enwiki/simplewiki-20211201-pages-articles-multistream.xml.bz2 index/simple-templates.xml.bz2` (Make sure you reference the dump file you just put in the folder)
2. Map titles: `./esa --map-titles index/simple-templates.xml.bz2 index/simple-titles.xml.bz2`
3. Strip articles: `./esa --strip index/simple-templates.xml.bz2 index/simple-stripped.xml.bz2 --title-exclusion-regex "^[^:]+:[^ ].+$" "^(january)|(february)|(march)|(april)|(may)|(june)|(july)|(august)|(september)|(november)|(december)] .+" "[0-9]{1,4}(s)?( bc)?" disambiguation wikiproject wikipedia`
4. Count links: `./esa --count-links index/simple-stripped.xml.bz2 index/simple-titles.xml.bz2 index/simple-stripped-links.xml.bz2`
5. Count Terms: `./esa --count-terms index/simple-stripped-links.xml.bz2 index/simple-stripped-links-terms.xml.bz2 --filter classic ascii lower singular stemmer --stemmer-depth 3 --stopwords en-default`
6. Repeat words: `./esa --repeat-content index/simple-stripped-links-terms.xml.bz2 index/simple-links-terms-repeated.xml.bz2 --repeat-title 4 --repeat-link 2`
7. Rare Words: `./esa --write-rare-words index/simple-links-terms-repeated.xml.bz2 index/simple-rare-words.txt 3 --filter classic ascii lower singular stemmer --stemmer-depth 3 --stopwords en-default`
=======
1. Resolve templates: `./esa.sh --resolve-templates enwiki/simplewiki-20211201-pages-articles-multistream.xml.bz2 index/simple-templates.xml.bz2` (Make sure you reference the dump file you just put in the folder)
2. Map titles: `./esa.sh --map-titles index/simple-templates.xml.bz2 index/simple-titles.xml.bz2`
3. Strip articles: `./esa.sh --strip index/simple-templates.xml.bz2 index/simple-stripped.xml.bz2 --title-exclusion-regex "^[^:]+:[^ ].+$" "^(january)|(february)|(march)|(april)|(may)|(june)|(july)|(august)|(september)|(november)|(december)] .+" "[0-9]{1,4}(s)?( bc)?" disambiguation wikiproject wikipedia`
4. Count links: `./esa.sh --count-links index/simple-stripped.xml.bz2 index/simple-titles.xml.bz2 index/simple-stripped-links.xml.bz2`
5. Count Terms: `./esa.sh --count-terms index/simple-stripped-links.xml.bz2 index/simple-stripped-links-terms.xml.bz2 --filter classic ascii lower singular stemmer --stemmer-depth 3 --stopwords en-default`
6. Repeat words: `./esa.sh --repeat-content index/simple-stripped-links-terms.xml.bz2 index/simple-links-terms-repeated.xml.bz2 --repeat-title 4 --repeat-link 2`
7. Rare Words: `./esa.sh --write-rare-words index/simple-links-terms-repeated.xml.bz2 index/simple-rare-words.txt 3 --filter classic ascii lower singular stemmer --stemmer-depth 3 --stopwords en-default`
8. Index! (If you're doing this multiple times, delete the index file): `./esa.sh --index index/simple-links-terms-repeated.xml.bz2 --threads 8 --batch-size 1000 --filter classic ascii lower singular stemmer --stemmer-depth 3 --stopwords en-default --rare-words index/simple-rare-words.txt --min-incoming-links 1 --min-outgoing-links 1 --min-word-length 3`
9. Spearman: `./esa.sh --spearman en-wordsim353 --filter classic ascii lower singular stemmer --stemmer-depth 3 --vector-limit 1800 --prune-dropoff 0.73`
10. Pearson: `./esa.sh --pearson en-lp50 --filter classic ascii lower singular stemmer --stemmer-depth 3 --preprocessor standard --min-word-length 3 --stopwords en-default --rare-words index/simple-rare-words.txt --vector-limit 450`

On Windows:

./esa.bat --resolve-templates enwiki\simplewiki-20210101-pages-articles-multistream.xml.bz2 index\simple-templates.xml.bz2

./esa.bat --map-titles index\simple-templates.xml.bz2 index\simple-titles.xml.bz2

./esa.bat --strip index\simple-templates.xml.bz2 index\simple-stripped.xml.bz2 --title-exclusion-regex "^[^:]+:[^ ].+$" "^(january)|(february)|(march)|(april)|(may)|(june)|(july)|(august)|(september)|(november)|(december)] .+" "[0-9]{1,4}(s)?( bc)?" disambiguation wikiproject wikipedia

./esa.bat --count-links index\simple-stripped.xml.bz2 index\simple-titles.xml.bz2 index\simple-stripped-links.xml.bz2
>>>>>>> 812ee24812c55af66168bbc70d4555d3d45efe46

#### Create the Lucene index

Now that the Wikipedia dump is downloaded and pre-processed, it must be indexed. 

```bash
./esa --index index/simple-links-terms-repeated.xml.bz2 --threads 8 --batch-size 1000 --filter classic ascii lower singular stemmer --stemmer-depth 3 --stopwords en-default --rare-words index/simple-rare-words.txt --min-incoming-links 1 --min-outgoing-links 1 --min-word-length 3
```

#### Check specific scores against standard datasets


_Spearman (word similarity/relatedness using Wordsim 353 dataset)_

```
./esa.sh --spearman en-wordsim353 --filter classic ascii lower singular stemmer --stemmer-depth 3 --vector-limit 1800 --prune-dropoff 0.73
```

_Pearson (document relatedness using LP50 dataset)_
```
./esa --pearson en-lp50 --filter classic ascii lower singular stemmer --stemmer-depth 3 --preprocessor standard --min-word-length 3 --stopwords en-default --rare-words index/simple-rare-words.txt --vector-limit 450
```
### About indexing

Indexing is done with Lucene in two steps.

First, all articles are indexed to a term-to-document index.
The documents are the concepts in ESA.

Second, the full-text index is inverted, so that each concept is mapped to all the terms that are important for that concept.
To find that index, the terms in the first index become a document in the second index.
Lucene further handles the indexing.

The class `Main` contains an `indexing` method.
Using that method, you can create a term to concept index (the first index).

The same class also contains a `createConceptTermIndex()` method, which is a bit more involved.
That method can be used to create the second index, which maps Wikipedia articles to their tokens.

#### Tweaking the indexing process

All kinds of tricks from Lucene can be used to tweak the indexing.
Maybe you will want to use a different Lucene Analyzer.
Taking a good look at Lucene documentation and the `WikiAnalyzer` class can be a good starting point for that.

### Analyzing

After indexing, you are ready to transform text to vectors.
Creating a concept vector from a text can be done with a Vectorizer, implemented in the class `Vectorizer`.

The vectorizer has a `vectorize(String text)` method, which transforms the text into a concept vector (`ConceptVector`).
Basically, the text is tokenized and searched for in the term-to-concept index.
The result is a list of Wikipedia articles, along with their numeric similarity to the vectorized text.
Two concept vectors can be easily compared to each other, using the `dotProduct` method.
The dot product of two concept vectors is a measure for the semantic similarity between the two texts those vectors are created from.

Calculating the semantic similarity between two texts directly is exactly what the semantic similarity tool (`SemanticSimilarityTool`) does.

### Automatic brainstorming

Finally, the automatic brainstormer is why I went through the effort to create an ESA implementation.
Starting from a text or a set of words, the brainstormer searches for words with a similar meaning.
That process can be repeated a couple of times to create a network of words that can be visualized with Graphviz.

The brainstormer is available in the class `be.vanoosten.esa.brainstorm.Brainstormer`.

## Theory

Wikipedia-based Explicit Semantic Analysis, as described by Gabrilovich and Markovitch.

ESA is well described in a scientific paper.

http://en.wikipedia.org/wiki/Explicit_semantic_analysis

http://www.cs.technion.ac.il/~gabr/resources/code/esa/esa.html

http://www.cs.technion.ac.il/~gabr/papers/ijcai-2007-sim.pdf
