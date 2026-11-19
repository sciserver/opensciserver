{
"comments": "simple update w/one table and alias",
"sql": "
UPDATE s SET Value = d.NumOfRows FROM statisticsTable s
INNER JOIN (
    SELECT UserId, COUNT(*) As NumOfRows FROM OtherTable GROUP BY UserId
) d ON s.UserId = d.UserId
WHERE id in (1,2,3);
",
"context": "[mydb:user1]",
"tables-used": [
    {
        "database": "[mydb:user1]",
	    "schema": "dbo",
	    "table_name": "statisticsTable",
	    "usage": "W"
    },
    {
        "database": "[mydb:user1]",
	    "schema": "dbo",
	    "table_name": "OtherTable",
	    "usage": "R"
    }
]
}
