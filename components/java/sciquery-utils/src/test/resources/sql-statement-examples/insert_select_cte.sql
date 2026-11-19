{
"comments":"statement combining INSERT .. SELECT.. with a CTE defining one of the 'tables' in the SELECT",
"sql":"

with res as
(select * from [mydb:user2]..tables123)
insert into baz.InsertTable
SELECT t1.A as anA,B,C,D
 FROM DB0.Sch1.[TABLE1]  t1, table2, res 
 ,  (select * from DB3.Sch3.table3 ) abc  WHERE C1='abcde' and d1=123
 
",
"context":"[mydb:user1]",
"tables-used":[
	{"database":"[mydb:user2]",
	"schema":"dbo",
	"table_name":"tables123",
	"usage":"R"}
,	{"database":"DB0",
	"schema":"Sch1",
	"table_name":"[TABLE1]",
	"usage":"R"}
,	{"database":"[mydb:user1]",
	"schema":"baz",
	"table_name":"InsertTable",
	"usage":"W"}
,	{"database":"[mydb:user1]",
	"schema":"dbo",
	"table_name":"table2",
	"usage":"R"}
,	{"database":"DB3",
	"schema":"Sch3",
	"table_name":"table3",
	"usage":"R"}
]
}
