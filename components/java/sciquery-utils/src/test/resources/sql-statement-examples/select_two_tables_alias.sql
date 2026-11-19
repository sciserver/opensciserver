{
"comments": "simple select w/two tables and aliases",
"sql": "
SELECT d.name, d.height, d.weight, b.name As 'breed'
FROM dog As d Inner Join breed As b On d.breed_id = b.id
WHERE breed.name = 'Newfoundland' ORDER BY dog.weight Desc;
",
"context": "[mydb:user1]",
"tables-used": [
	{
        "database": "[mydb:user1]",
	    "schema": "dbo",
	    "table_name": "breed",
	    "usage": "R"
    },
    {
        "database": "[mydb:user1]",
	    "schema": "dbo",
	    "table_name": "dog",
	    "usage": "R"
    }    
]
}
