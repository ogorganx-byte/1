package com.buildfinance.app

import android.app.Activity
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val Navy=Color(0xFF14243E)
private val Blue=Color(0xFF3868DB)
private val Mint=Color(0xFF16876B)
private val Rose=Color(0xFFCB5265)
private val Muted=Color(0xFF79869A)
private val Canvas=Color(0xFFF7F9FC)
private val Gold=Color(0xFFD6B780)
private val Shape=RoundedCornerShape(22.dp)
private fun euros(n:Double)=NumberFormat.getCurrencyInstance(Locale.GERMANY).format(n)
private fun date(t:Long)=SimpleDateFormat("dd.MM.yyyy",Locale.getDefault()).format(Date(t))
data class Record(val id:Long,val kind:String,val category:String,val project:String,val note:String,val amount:Double,val created:Long)
class FinanceDb(activity:Activity):SQLiteOpenHelper(activity,"finance.db",null,1){
 override fun onCreate(db:SQLiteDatabase){db.execSQL("CREATE TABLE entries(id INTEGER PRIMARY KEY AUTOINCREMENT,kind TEXT NOT NULL,category TEXT NOT NULL,project TEXT NOT NULL,note TEXT NOT NULL,amount REAL NOT NULL,created INTEGER NOT NULL)")}
 override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int){}
 fun all():List<Record>{
  val result=mutableListOf<Record>()
  readableDatabase.rawQuery("SELECT id,kind,category,project,note,amount,created FROM entries ORDER BY created DESC,id DESC",null).use{c->
   while(c.moveToNext())result.add(Record(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getDouble(5),c.getLong(6)))
  }
  return result
 }
 fun save(r:Record?,kind:String,category:String,project:String,note:String,amount:Double){
  val v=ContentValues().apply{put("kind",kind);put("category",category);put("project",project);put("note",note);put("amount",amount);if(r==null)put("created",System.currentTimeMillis())}
  if(r==null)writableDatabase.insert("entries",null,v) else writableDatabase.update("entries",v,"id=?",arrayOf(r.id.toString()))
 }
 fun remove(r:Record){writableDatabase.delete("entries","id=?",arrayOf(r.id.toString()))}
}
class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  val db=FinanceDb(this)
  window.statusBarColor=android.graphics.Color.rgb(247,249,252)
  window.navigationBarColor=android.graphics.Color.rgb(247,249,252)
  window.decorView.systemUiVisibility=android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
  setContent{FinanceApp(db)}
 }
}
@Composable private fun FinanceApp(db:FinanceDb){
 var tab by remember{mutableIntStateOf(0)}
 var refresh by remember{mutableIntStateOf(0)}
 var selected by remember{mutableStateOf<Record?>(null)}
 var editing by remember{mutableStateOf(false)}
 var defaultCategory by remember{mutableStateOf("Материалы")}
 var projectFilter by remember{mutableStateOf<String?>(null)}
 val entries=remember(refresh){db.all()}
 val projects=entries.map{it.project}.distinct()
 val income=entries.filter{it.kind=="Доход"}.sumOf{it.amount}
 val spent=entries.filter{it.kind!="Доход"}.sumOf{it.amount}
 MaterialTheme(colorScheme=lightColorScheme(primary=Blue,background=Canvas,surface=Color.White,onSurface=Navy,onBackground=Navy)){
  Scaffold(
   modifier=Modifier.fillMaxSize(),containerColor=Canvas,contentWindowInsets=WindowInsets.safeDrawing,
   topBar={
    Row(Modifier.fillMaxWidth().background(Canvas).statusBarsPadding().padding(horizontal=20.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){
     Surface(shape=RoundedCornerShape(14.dp),color=Navy,modifier=Modifier.size(44.dp)){
      Box(contentAlignment=Alignment.Center){Icon(Icons.Outlined.AccountBalance,contentDescription=null,tint=Gold,modifier=Modifier.size(26.dp))}
     }
     Spacer(Modifier.width(12.dp))
     Column(Modifier.weight(1f)){
      Text("BUILD FINANCE",fontSize=19.sp,fontWeight=FontWeight.Bold,letterSpacing=0.5.sp,color=Navy)
      Text("Финансы строительных проектов",fontSize=11.sp,color=Muted)
     }
     IconButton(onClick={tab=3}){Icon(Icons.Outlined.MoreHoriz,contentDescription="Отчёты",tint=Navy)}
    }
   },
   bottomBar={
    NavigationBar(containerColor=Color.White,tonalElevation=0.dp,windowInsets=WindowInsets.navigationBars){
     val tabs=listOf(Triple("Главная",Icons.Outlined.Home,0),Triple("Объекты",Icons.Outlined.Apartment,1),Triple("Операции",Icons.Outlined.ReceiptLong,2),Triple("Отчёты",Icons.Outlined.BarChart,3))
     tabs.forEach{(label,icon,index)->
      NavigationBarItem(selected=tab==index,onClick={tab=index},icon={Icon(icon,contentDescription=label)},label={Text(label,fontSize=11.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Blue,selectedTextColor=Blue,indicatorColor=Color(0xFFEDF2FF),unselectedIconColor=Muted,unselectedTextColor=Muted))
     }
    }
   }
  ){padding->
   LazyColumn(modifier=Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=8.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    when(tab){
     0->{
      item{
       Column(Modifier.fillMaxWidth().background(Navy,Shape).padding(horizontal=22.dp,vertical=19.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
         Text("ОБЩИЙ БАЛАНС",fontSize=12.sp,color=Color(0xFFBCC9DC),fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
         Icon(Icons.Outlined.AccountBalanceWallet,contentDescription=null,tint=Gold)
        }
        Spacer(Modifier.height(9.dp))
        Text(euros(income-spent),fontSize=32.sp,fontWeight=FontWeight.Bold,color=Color.White,maxLines=1)
        Text("По всем объектам",fontSize=13.sp,color=Color(0xFFBCC9DC))
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color=Color(0xFF35445C))
        Spacer(Modifier.height(13.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
         SummaryMetric("ОБЪЕКТЫ",projects.size.toString(),Color.White,Modifier.weight(1f))
         SummaryMetric("ДОХОДЫ",euros(income),Color(0xFF70E0B9),Modifier.weight(1f))
         SummaryMetric("РАСХОДЫ",euros(spent),Color(0xFFFFA7AE),Modifier.weight(1f))
        }
       }
      }
      item{
       Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        QuickAction("Операция",Icons.Outlined.AddCircle,Modifier.weight(1f),true){selected=null;defaultCategory="Материалы";editing=true}
        QuickAction("Объекты",Icons.Outlined.Apartment,Modifier.weight(1f)){tab=1}
        QuickAction("Работники",Icons.Outlined.Groups,Modifier.weight(1f)){selected=null;defaultCategory="Работники";editing=true}
        QuickAction("Материалы",Icons.Outlined.Inventory2,Modifier.weight(1f)){selected=null;defaultCategory="Материалы";editing=true}
       }
      }
      item{SectionHeading("Объекты","Все объекты"){tab=1}}
      if(projects.isEmpty())item{EmptyCard("Пока нет объектов","Добавь первую операцию и укажи объект.")}
      else items(projects.take(3)){p->ProjectCard(p,entries){projectFilter=p;tab=2}}
      item{SectionHeading("Последние операции","Все операции"){projectFilter=null;tab=2}}
      if(entries.isEmpty())item{EmptyCard("Операций пока нет","Добавь первый доход или расход.")}
      else items(entries.take(5)){r->RecordCard(r){selected=r;editing=true}}
     }
     1->{
      item{Text("Объекты",fontSize=27.sp,fontWeight=FontWeight.Bold)}
      item{Text("Доходы, расходы и результат по каждому проекту",fontSize=13.sp,color=Muted)}
      item{Button(onClick={selected=null;editing=true},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)){Icon(Icons.Outlined.Add,contentDescription=null);Spacer(Modifier.width(8.dp));Text("Новая операция для объекта")}}
      if(projects.isEmpty())item{EmptyCard("Объектов пока нет","Начни с первой операции.")}
      else items(projects){p->ProjectCard(p,entries){projectFilter=p;tab=2}}
     }
     2->{
      item{Text("Операции",fontSize=27.sp,fontWeight=FontWeight.Bold)}
      item{Button(onClick={selected=null;editing=true},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)){Icon(Icons.Outlined.Add,contentDescription=null);Text(" Добавить операцию")}}
      if(projectFilter!=null)item{TextButton(onClick={projectFilter=null}){Text("Объект: "+projectFilter+"   ×")}}
      val filtered=entries.filter{projectFilter==null||it.project==projectFilter}
      if(filtered.isEmpty())item{EmptyCard("Нет операций","Добавь новую запись.")}
      else items(filtered){r->RecordCard(r){selected=r;editing=true}}
     }
     else->{
      item{Text("Отчёты",fontSize=27.sp,fontWeight=FontWeight.Bold)}
      item{Surface(shape=Shape,color=Color.White){Column(Modifier.fillMaxWidth().padding(20.dp)){Text("Чистый результат",color=Muted);Spacer(Modifier.height(8.dp));Text(euros(income-spent),fontSize=30.sp,fontWeight=FontWeight.Bold,color=if(income>=spent)Mint else Rose)}}}
      item{SectionHeading("Расходы по категориям",null){}}
      val grouped=entries.filter{it.kind!="Доход"}.groupBy{it.category}
      if(grouped.isEmpty())item{EmptyCard("Расходов нет","Здесь будет структура расходов.")}
      else items(grouped.toList().sortedByDescending{it.second.sumOf{r->r.amount}}){(cat,ops)->
       Surface(shape=Shape,color=Color.White){Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text(cat,modifier=Modifier.weight(1f),fontWeight=FontWeight.Medium);Text(euros(ops.sumOf{it.amount}),color=Rose,fontWeight=FontWeight.Bold)}}
      }
      item{SectionHeading("Выплаты работникам",null){}}
      val workers=entries.filter{it.kind!="Доход"&&it.category=="Работники"}.groupBy{it.note.ifBlank{"Без имени"}}
      if(workers.isEmpty())item{EmptyCard("Нет выплат","Записывай имя работника в описании.")}
      else items(workers.toList()){(name,ops)->Surface(shape=Shape,color=Color.White){Row(Modifier.fillMaxWidth().padding(18.dp)){Text(name,Modifier.weight(1f));Text(euros(ops.sumOf{it.amount}),color=Rose)}}}
     }
    }
   }
  }
  if(editing){
   Editor(selected,defaultCategory,onDismiss={editing=false},onSave={kind,cat,project,note,amount->
    db.save(selected,kind,cat,project,note,amount);refresh++;editing=false
   },onDelete={selected?.let{db.remove(it)};refresh++;editing=false})
  }
 }
}
@Composable private fun SummaryMetric(label:String,value:String,color:Color,modifier:Modifier=Modifier){
 Column(modifier){Text(value,fontWeight=FontWeight.Bold,fontSize=15.sp,color=color,maxLines=1,overflow=TextOverflow.Ellipsis);Spacer(Modifier.height(5.dp));Text(label,fontSize=10.sp,color=Color(0xFFBCC9DC))}
}
@Composable private fun QuickAction(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier=Modifier,dark:Boolean=false,action:()->Unit){
 Surface(onClick=action,modifier=modifier.height(80.dp),shape=RoundedCornerShape(17.dp),color=if(dark)Navy else Color.White,border=if(dark)null else BorderStroke(1.dp,Color(0xFFE8ECF2))){
  Column(Modifier.fillMaxSize().padding(6.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
   Icon(icon,contentDescription=null,tint=if(dark)Color.White else Navy,modifier=Modifier.size(25.dp))
   Spacer(Modifier.height(7.dp))
   Text(label,fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=if(dark)Color.White else Navy,maxLines=1)
  }
 }
}
@Composable private fun SectionHeading(title:String,link:String?,action:()->Unit){
 Row(Modifier.fillMaxWidth().padding(top=7.dp),verticalAlignment=Alignment.CenterVertically){
  Text(title,fontSize=20.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
  if(link!=null)TextButton(onClick=action){Text(link+" ›",fontSize=12.sp,color=Muted)}
 }
}
@Composable private fun EmptyCard(title:String,subtitle:String){
 Surface(shape=Shape,color=Color.White,border=BorderStroke(1.dp,Color(0xFFE9EDF3))){
  Column(Modifier.fillMaxWidth().padding(20.dp)){
   Text(title,fontWeight=FontWeight.Bold,fontSize=16.sp)
   Spacer(Modifier.height(7.dp))
   Text(subtitle,color=Muted,fontSize=13.sp)
  }
 }
}
@Composable private fun ProjectCard(name:String,entries:List<Record>,action:()->Unit){
 val ops=entries.filter{it.project==name}
 val inc=ops.filter{it.kind=="Доход"}.sumOf{it.amount}
 val out=ops.filter{it.kind!="Доход"}.sumOf{it.amount}
 Surface(onClick=action,shape=Shape,color=Color.White,border=BorderStroke(1.dp,Color(0xFFE9EDF3))){
  Column(Modifier.fillMaxWidth().padding(17.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Apartment,contentDescription=null,tint=Blue,modifier=Modifier.size(20.dp));Spacer(Modifier.width(9.dp));Text(name,fontSize=17.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Icon(Icons.Outlined.ChevronRight,contentDescription=null,tint=Muted)}
   Spacer(Modifier.height(11.dp))
   Text("Финансовый результат",fontSize=12.sp,color=Muted)
   Text(euros(inc-out),fontSize=24.sp,fontWeight=FontWeight.Bold,color=if(inc>=out)Mint else Rose)
   Spacer(Modifier.height(10.dp))
   Row(Modifier.fillMaxWidth().background(Canvas,RoundedCornerShape(12.dp)).padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){
    Text("Доходы "+euros(inc),fontSize=11.sp,color=Mint,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
    Spacer(Modifier.width(8.dp));Text("Расходы "+euros(out),fontSize=11.sp,color=Rose,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
   }
  }
 }
}
@Composable private fun RecordCard(r:Record,action:()->Unit){
 val positive=r.kind=="Доход"
 Surface(onClick=action,shape=Shape,color=Color.White,border=BorderStroke(1.dp,Color(0xFFE9EDF3))){
  Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
   Box(Modifier.size(45.dp).background(if(positive)Color(0xFFE7F8F1) else Color(0xFFFFEEF1),RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center){
    Icon(if(positive)Icons.Outlined.SouthWest else Icons.Outlined.NorthEast,contentDescription=null,tint=if(positive)Mint else Rose)
   }
   Spacer(Modifier.width(12.dp))
   Column(Modifier.weight(1f)){
    Text(r.category,fontWeight=FontWeight.SemiBold,fontSize=15.sp)
    Text(r.project+" · "+date(r.created),fontSize=11.sp,color=Muted)
    if(r.note.isNotBlank())Text(r.note,fontSize=11.sp,color=Muted,maxLines=1)
   }
   Text((if(positive)"+" else "−")+euros(r.amount),fontWeight=FontWeight.Bold,fontSize=14.sp,color=if(positive)Mint else Rose)
  }
 }
}
@Composable private fun Editor(record:Record?,defaultCategory:String,onDismiss:()->Unit,onSave:(String,String,String,String,Double)->Unit,onDelete:()->Unit){
 var kind by remember(record){mutableStateOf(record?.kind?:"Расход")}
 var category by remember(record){mutableStateOf(record?.category?:defaultCategory)}
 var project by remember(record){mutableStateOf(record?.project?:"")}
 var note by remember(record){mutableStateOf(record?.note?:"")}
 var amount by remember(record){mutableStateOf(record?.amount?.toString()?:"")}
 var showDelete by remember{mutableStateOf(false)}
 val cats=if(kind=="Доход")listOf("Оплата клиента","Аванс клиента","Прочее") else listOf("Материалы","Работники","Транспорт","Инструменты","Прочее")
 LaunchedEffect(kind){if(category !in cats)category=cats.first()}
 AlertDialog(onDismissRequest=onDismiss,title={Text(if(record==null)"Новая операция" else "Редактировать",fontWeight=FontWeight.Bold)},text={
  Column(verticalArrangement=Arrangement.spacedBy(11.dp),modifier=Modifier.verticalScroll(rememberScrollState())){
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Расход","Доход").forEach{v->FilterChip(selected=kind==v,onClick={kind=v},label={Text(v)})}}
   Text("Категория",fontSize=12.sp,color=Muted)
   Column{cats.chunked(3).forEach{chunk->Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){chunk.forEach{c->FilterChip(selected=category==c,onClick={category=c},label={Text(c,fontSize=11.sp)})}}}}
   OutlinedTextField(value=project,onValueChange={project=it},label={Text("Объект / адрес")},singleLine=true,modifier=Modifier.fillMaxWidth())
   OutlinedTextField(value=note,onValueChange={note=it},label={Text("Описание / работник")},singleLine=true,modifier=Modifier.fillMaxWidth())
   OutlinedTextField(value=amount,onValueChange={amount=it},label={Text("Сумма, €")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true,modifier=Modifier.fillMaxWidth())
   if(record!=null)TextButton(onClick={showDelete=true}){Text("Удалить операцию",color=Rose)}
  }
 },confirmButton={
  val parsed=amount.replace(',','.').toDoubleOrNull()
  Button(onClick={onSave(kind,category,project.trim(),note.trim(),parsed?:0.0)},enabled=project.isNotBlank()&&parsed!=null&&parsed.isFinite()&&parsed>0){Text("Сохранить")}
 },dismissButton={TextButton(onClick=onDismiss){Text("Отмена")}})
 if(showDelete)AlertDialog(onDismissRequest={showDelete=false},title={Text("Удалить операцию?")},confirmButton={TextButton(onClick=onDelete){Text("Удалить",color=Rose)}},dismissButton={TextButton(onClick={showDelete=false}){Text("Отмена")}})
}
