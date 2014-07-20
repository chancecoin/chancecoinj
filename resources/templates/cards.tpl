<#function getCardSuit card>
  <#if card[1]="D">
    <#return "diams">
  </#if>
  <#if card[1]="H">
    <#return "hearts">
  </#if>
  <#if card[1]="S">
    <#return "spades">
  </#if>
  <#if card[1]="C">
    <#return "clubs">
  </#if>
</#function>

<#function getCardRank card>
  <#if card[0]="T">
    <#return 10>
  <#else>
    <#return card[0]>
  </#if>
</#function>
