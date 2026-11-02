{
"comments":"select including a CTE",
"sql":"
with res as
(select * from tables123)

SELECT t1.A,t2.B,t2.C,res.D
 FROM Sch1.TABLE1  t1, table2 t2 , res 
 ,  (select * from DB3.Sch3.table3 ) subselect WHERE C1='abcde' and d1=123
 
 ",
"context":"[mydb:user1]",
"tables-used":[
	{"database":"[mydb:user1]",
	"schema":"dbo",
	"table_name":"tables123",
	"usage":"R"}
,	{"database":"[mydb:user1]",
	"schema":"Sch1",
	"table_name":"TABLE1",
	"usage":"R"}
,	{"database":"DB3",
	"schema":"Sch3",
	"table_name":"table3",
	"usage":"R"}
,	{"database":"[mydb:user1]",
	"schema":"dbo",
	"table_name":"table2",
	"usage":"R"}
]
}
