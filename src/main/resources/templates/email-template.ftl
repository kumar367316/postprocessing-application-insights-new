<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>post processing mail</title>
<style>
table {
  border-collapse: collapse;
  width: 48%;
  border: 1px solid #ddd;
}

th, td {
  text-align: left;
  padding: 6px;
}
</style>
</head>
<body>
       <div>
		   <br> SmartComm Post Processing has completed successfully
	    </div>
	<div>
	  <div>Summary</div>
      <table>
         <tr>
            <th>File Type</th>
             <th>Total</th>
         </tr>
           <#list mailResponseList as mailResponse>
           <tr>
              <td>${mailResponse.fileType}</td>
              <td>${mailResponse.totalSize}</td>
           </tr>
         </#list>
       </table> 
	 </div>
<br/>
</body>
</html>