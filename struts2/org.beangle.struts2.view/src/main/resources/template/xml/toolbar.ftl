[#ftl/]
<div id="${tag.id}"></div>
<script type="text/javascript">
bg.ready(function(){
	bar = bg.ui.toolbar("${tag.id}"[#if tag.title??],'${tag.title?replace("'","\"")}'[/#if]);
	${tag.body}
	bar.addHr();
});
</script>