package org.apache.ibatis.submitted.automapping;

import java.util.List;

public interface Mapper {

	User getUser(Integer id);

	User getUserWithPhoneNumber(Integer id);

	User getUserWithPets_Inline(Integer id);

	User getUserWithPets_External(Integer id);

	List<Book> getBooks();

	Article getArticle();
}
