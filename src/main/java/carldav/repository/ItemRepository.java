/*
 * Copyright 2006 Open Source Applications Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package carldav.repository;

import carldav.entity.Item;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ItemRepository extends CrudRepository<Item, Long>, JpaSpecificationExecutor {

    List<Item> findByCollectionIdAndType(Long id, Item.Type type);

    List<Item> findByCollectionId(Long id);

    @Query("select i from Item i where i.collection.name = ?1 and i.name = ?2 and i.collection.owner.email = ?#{ principal.username }")
    Item findByCurrentOwnerEmailAndCollectionNameAndName(String collectionName, String name);

}
