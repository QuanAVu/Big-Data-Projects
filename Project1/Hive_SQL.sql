-- Quan Vu, All SQL queries needed for HIVE in project 1
-- The queries are written in a format that works for Hive beeline
-- so it is a little messy and hard to read.

-- Tables created:

CREATE TABLE PAGE_VIEW
(DOMAIN_CODE STRING, PAGE_TITLE STRING, COUNT_VIEWS INT, TOTAL_RESPONSE_SIZE INT)
ROW FORMAT DELIMITED	-- Each line represents a record
FIELDS TERMINATED BY ' '; -- Each element for each column is separated by a space
-- Load 
LOAD DATA LOCAL INPATH '/home/quanvu/Project1-Data/pageViews/octo20-fullday' INTO TABLE PAGE_VIEW;

/* Three different tables for three countries*/
-- Rush hour table for UK 
CREATE TABLE PAGE_VIEW_UK
(DOMAIN_CODE STRING, PAGE_TITLE STRING, COUNT_VIEWS INT, TOTAL_RESPONSE_SIZE INT)
ROW FORMAT DELIMITED	
FIELDS TERMINATED BY ' ';

LOAD DATA LOCAL INPATH '/home/quanvu/Project1-Data/pageViews/octo20-UK' INTO TABLE PAGE_VIEW_UK;

-- Rush hour table for US
CREATE TABLE PAGE_VIEW_US
(DOMAIN_CODE STRING, PAGE_TITLE STRING, COUNT_VIEWS INT, TOTAL_RESPONSE_SIZE INT)
ROW FORMAT DELIMITED	
FIELDS TERMINATED BY ' ';

LOAD DATA LOCAL INPATH '/home/quanvu/Project1-Data/pageViews/octo20-US' INTO TABLE PAGE_VIEW_US;

-- Rush hour table for AU 
CREATE TABLE PAGE_VIEW_AU
(DOMAIN_CODE STRING, PAGE_TITLE STRING, COUNT_VIEWS INT, TOTAL_RESPONSE_SIZE INT)
ROW FORMAT DELIMITED	
FIELDS TERMINATED BY ' ';

LOAD DATA LOCAL INPATH '/home/quanvu/Project1-Data/pageViews/octo20-AU' INTO TABLE PAGE_VIEW_AU;


CREATE TABLE CLICKSTREAM
(ORIGIN STRING, INTERNAL STRING, RELATION STRING, CLICKS INT)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\t';
-- Load local data into table 
LOAD DATA LOCAL INPATH '/home/quanvu/Project1-Data/clickstream-enwiki-2020-09.tsv' INTO TABLE clickstream;

-- Query 1
/*	Get the top 10 highest viewed English wikipedia pages.
	First sum is the summing of count views from all 24 hours
	page_view files.
	Second sum is summing matching articles from both en and en.m */
-- Can add the query result onto HDFS: 
INSERT OVERWRITE DIRECTORY '/user/hive/output/Query1'
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '  '
SELECT NewTable.PAGE_TITLE, SUM(NewTable.SUM_VIEWS) AS TOTAL_VIEWS
FROM (SELECT DOMAIN_CODE, PAGE_TITLE, SUM(COUNT_VIEWS) AS SUM_VIEWS
      FROM PAGE_VIEW
      WHERE DOMAIN_CODE= 'en' OR DOMAIN_CODE= 'en.m'
      GROUP BY DOMAIN_CODE, PAGE_TITLE) AS NewTable
GROUP BY NewTable.PAGE_TITLE
ORDER BY TOTAL_VIEWS DESC
LIMIT 10;

-- Query 2
/*	Get the top 10 English wikipedia articles that have internal links
	that were clicked the most (highest fraction).*/
INSERT OVERWRITE DIRECTORY '/user/hive/output/Query2'
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '  '
SELECT Table2.origin AS referrer, Table2.internal AS requested, ROUND(Table2.clicks/Table1.total1, 2) AS fraction
FROM (SELECT origin, SUM(clicks) AS total1 FROM CLICKSTREAM WHERE relation='link' GROUP BY origin ORDER BY total1 DESC LIMIT 1000) AS Table1, (SELECT * FROM CLICKSTREAM WHERE relation='link' ORDER BY clicks DESC LIMIT 1000) AS Table2
WHERE Table1.origin = Table2.origin
ORDER BY fraction DESC 
LIMIT 10;


-- Query 3
/* 	Get a series of articles starting from Hotel_California
	that have the highest fraction of clicks (the clicks number is not
	accurate because it is a combination of both clicks that are 
	from the chain and clicks from when user search for the last
	internal article as an original article)*/
INSERT OVERWRITE DIRECTORY '/user/hive/output/Query3'
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '  '
SELECT NewTable.referrer AS referrer, NewTable.requested AS requested1, CLICKSTREAM.internal AS requested2, ROUND(CLICKSTREAM.clicks/NewTable.total, 2) AS final_fraction
FROM CLICKSTREAM, (SELECT Table2.origin AS referrer, Table2.internal AS requested, Table1.total1 AS total, ROUND(Table2.clicks/Table1.total1, 2) AS fraction
FROM (SELECT origin, SUM(clicks) AS total1 FROM CLICKSTREAM WHERE origin='Hotel_California' AND relation='link' GROUP BY origin ORDER BY total1 DESC LIMIT 1000) AS Table1, (SELECT * FROM CLICKSTREAM WHERE origin='Hotel_California' AND relation='link' ORDER BY clicks DESC LIMIT 1000) AS Table2
WHERE Table1.origin = Table2.origin
ORDER BY fraction DESC LIMIT 1) AS NewTable 
WHERE NewTable.requested = CLICKSTREAM.origin
ORDER BY final_fraction DESC
LIMIT 10;


-- Query 4
/* 	Get the highest viewed English wikipedia article in October 20, 2020
	for US, UK, AU based on Internet rush hours of each country.
	We have three different queries for our three tables 
*/

-- Query to get max page view of articles for UK 
-- For queries on US and AU just change Page_Title to US or AU 
-- and the nested FROM clause to appropriate country
INSERT OVERWRITE DIRECTORY '/user/hive/output/Query4UK'
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '  '

SELECT NewTable.PAGE_TITLE AS AU_PAGE_TITLE, SUM(NewTable.SUM_VIEWS) AS TOTAL_VIEWS
FROM (SELECT DOMAIN_CODE, PAGE_TITLE, SUM(COUNT_VIEWS) AS SUM_VIEWS
      FROM PAGE_VIEW_AU
      WHERE DOMAIN_CODE= 'en' OR DOMAIN_CODE= 'en.m'
      GROUP BY DOMAIN_CODE, PAGE_TITLE) AS NewTable
GROUP BY NewTable.PAGE_TITLE
ORDER BY TOTAL_VIEWS DESC
LIMIT 10;



-- Query 5
-- TODO: Populate table with about 70 fields? -- maybe use mapreduce to down size the number of columns?
-- Find the average vandelized/revised page (for October 20,2020) by looking at the cumulative revision count (enwiki revision create only)
-- Once the page is found, pick a revision created that the time of creation to the time it got deleted is short so we can find
-- the number of page views in that time. (before picking the revision, search for the revision that had been deleted) 
-- Have to compare page_view and revision tables 

-- Query 6 Use MapReduce??? -> Trying to find the average amount of US editors for enwiki

