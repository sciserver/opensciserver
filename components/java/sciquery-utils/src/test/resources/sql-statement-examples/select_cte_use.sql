{
"comments":"including a use statement in a select with a CTE to update the current context.\n TBD do we need the GO after the use or should it actually not be there?",
"sql":"
use foo
GO

with res as
(select * from [mydb:jkim485]..tables123)

SELECT t1.A as anA,B,C,D
 FROM DB0.Sch1.[TABLE1]  t1, table2, res 
 ,  (select * from DB3.Sch3.table3 ) abc  WHERE C1='abcde' and d1=123
",
"context":"[mydb:jkim485]",
"tables-used":[
	{"database":"[mydb:jkim485]",
	"schema":"dbo",
	"table_name":"tables123",
	"usage":"R"}
,	{"database":"DB0",
	"schema":"Sch1",
	"table_name":"[TABLE1]",
	"usage":"R"}
,	{"database":"foo",
	"schema":"dbo",
	"table_name":"table2",
	"usage":"R"}
,	{"database":"DB3",
	"schema":"Sch3",
	"table_name":"table3",
	"usage":"R"}
]
}
