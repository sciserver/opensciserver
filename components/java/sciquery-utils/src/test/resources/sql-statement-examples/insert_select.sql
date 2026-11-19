{"comments":"insert ... select... pattern",
"sql":"
insert into MyDB..Mytable 
select * from foo.bar.ATable
",
"context":"[mydb:user1]",
"tables-used":[
	{"database":"MyDB",
	"schema":"dbo",
	"table_name":"Mytable",
	"usage":"W"}
,	{"database":"foo",
	"schema":"bar",
	"table_name":"ATable",
	"usage":"R"}
]
}
