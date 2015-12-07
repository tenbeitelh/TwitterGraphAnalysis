REGISTER hdfs:///lib/elephantbird/elephant-bird-core-4.10.jar;
REGISTER hdfs:///lib/elephantbird/elephant-bird-pig-4.10.jar;
REGISTER hdfs:///lib/elephantbird/elephant-bird-hadoop-compat-4.10.jar;
REGISTER hdfs:///lib/elephantbird/google-collections-1.0.jar;
REGISTER hdfs:///lib/elephantbird/json-simple-1.1.jar;
REGISTER hdfs:///lib/tenbeitel/CustomPigUDFs-0.0.1-SNAPSHOT.jar;

twitter_files_of_month = LOAD '$input' USING com.twitter.elephantbird.pig.load.JsonLoader('-nestedLoad') AS (json:map[]);

non_empty_tweets = FILTER twitter_files_of_month BY (json#'text' IS NOT NULL);
non_empty_tweets2 = FILTER non_empty_tweets BY SIZE((chararray)json#'text')>0;

de_tweets = FILTER non_empty_tweets2 BY (json#'lang' == '$lang');

distinct_de_tweets = DISTINCT de_tweets;

stopwords = FOREACH de_tweets GENERATE de.hs.osnabrueck.tenbeitel.pig.udf.StopWordUDF(json#'text');
stopword = LIMIT stopwords 1;
DUMP stopword;