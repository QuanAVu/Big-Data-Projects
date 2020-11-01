-- Quan Vu, All SQL queries needed for HIVE in project 1

-- Create new table, here is an example:
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
	that were clicked the most.*/
SELECT * 
FROM CLICKSTREAM
WHERE relation='link'
ORDER BY clicks DESC
LIMIT 10;

