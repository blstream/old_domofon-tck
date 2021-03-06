package domofon.tck

trait DomofonTck extends BaseTckTest
  with PostContactTest
  with GetContactsTest
  with GetContactItemTest
  with RemoveContactItemTest
  with SseContactsTest
  with ChangeContactDeputyTest
  with ChangeContactIsImportantTest
  with DomofonYamlTest
  with SendContactNotificationTest
  with PostCategoryTest
  with GetCategoriesTest
  with GetCategoryItemTest
  with SendCategoryNotificationTest
  with RemoveCategoryItemTest
  with CategoryMessagesTest
  with AdminLoginTest
  with AdminOpsTest