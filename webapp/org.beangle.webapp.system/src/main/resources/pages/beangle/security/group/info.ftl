[#ftl]
[@b.head/]
[@b.toolbar title='info.group']
bar.addBack("${b.text("action.back")}");
[/@]
<table class="infoTable">
	<tr>
	 <td class="title">${b.text("common.name")}:</td>
	 <td class="content"> ${userGroup.name}</td>
	 <td class="title">${b.text("common.creator")}:</td>
	 <td class="content">${userGroup.owner.name!}  </td>
	</tr>
	<tr>
	 <td class="title" >${b.text("common.createdAt")}:</td>
	 <td class="content">${userGroup.createdAt?string("yyyy-MM-dd")}</td>
	 <td class="title" >${b.text("common.updatedAt")}:</td>
	 <td class="content">${userGroup.updatedAt?string("yyyy-MM-dd")}</td>
	</tr>
	<tr>
	<td class="title" >适用身份:</td>
	<td  class="content">${userGroup.category.name}</td>
	<td class="title" >&nbsp;${b.text("common.status")}:</td>
	<td class="content">
		[#if userGroup.enabled] ${b.text("action.activate")}
		[#else]${b.text("action.freeze")}
		[/#if]
	</td>
	</tr>
	<tr>
	<td class="title" >${b.text("group.users")}:</td>
	<td  class="content" colspan="3">[#list userGroup.members?sort_by(["user","name"]) as m] ${m.user.fullname}(${m.user.name})&nbsp;[/#list]</td>
	</tr>
	<tr>
	<td class="title" >${b.text("common.description")}:</td>
	<td  class="content" colspan="3">${userGroup.description!}</td>
	</tr>
	<tr>
		<td colspan="4">[@b.div href="restriction!info?restriction.holder.id=${userGroup.id}&restrictionType=group" /]</td>
	</tr>
</table>
[@b.foot/]
