[#ftl]
[@b.head/]

<table id="resourceInfoBar"></table>
	 <table class="infoTable">
	   <tr>
		 <td class="title" >${b.text("common.description")}:</td>
		 <td  class="content">${pattern.description!}</td>
		 <td class="title" >使用参数:</td>
		 <td class="content" colspan="3">[#list pattern.paramGroup.params as param](${param.name})${param.description}<br/>[/#list]</td>
	   </tr>
	   <tr>
		 <td class="title" >限制模式:</td>
		 <td class="content" colspan="3">${pattern.content!}</td>
	   </tr>
	   <tr>
	   </tr>
	  </table>
  <script type="text/javascript">
   var bar = bg.ui.toolbar('resourceInfoBar','${b.text("security.restrictionPattern.info")}');
   bar.setMessage('[@b.messages/]');
   bar.addBack("${b.text("action.back")}");
  </script>
 
[@b.foot/]
