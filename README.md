## spring-boot-hibernate-bidirectional-many-to-one-relationship-mapping

1- Implement hibernate bidirectional many-to-one relational mapping <br/>
2- ER diagram :  <br/>
NOT : Many OrderItem related to one parent CustomerOrder entity <br/>

![many_to_one_er_diagram](doc/many_to_one_er_diagram.png) <br/>


3- Start Spring Boot application with a specific profile such as "-Dspring.profiles.active=dev" . <br/>
4- swagger-ui can be accessed from URL : http://localhost:8080/customer-info/swagger-ui/ <br/><br/>

![swagger_ui](doc/swagger_ui.png) <br/>
<br/>

### Tech Stack
Java 11 <br/>
H2 Database Engine <br/>
spring boot <br/>
spring data jpa <br/>
spring web <br/>
hibernate <br/>
logback <br/>
maven <br/>
junit <br/>
springfox-swagger-ui <br/>
datasource-proxy <br/>
<br/>


## API OPERATIONS
### Save customer sucessfully to database

Method : HTTP.POST <br/>
URL : localhost:8080/customer-info/customer/save <br/>

Request : 
<pre>
curl --location --request POST 'localhost:8080/customer-info/customer/save' \
--header 'Content-Type: application/json' \
--data-raw '{
    "name": "name1",
    "age": 1,
    "shippingAddress": {
        "streetName": "software",
        "city": "ankara",
        "country": "TR"
    }
}'
</pre><br/>

Response : 

HTTP response code 200 <br/>
<pre>
{
    "id": 1,
    "name": "name1",
    "age": 1,
    "shippingAddress": {
        "id": 1,
        "streetName": "software",
        "city": "ankara",
        "country": "TR"
    }
}
</pre>

### Create a new CustomerOrder with many OrderItems for Customer id = 1.

Method : HTTP.POST <br/>
URL : localhost:8080/customer-info/customerorder/save <br/>

Request : 
<pre>
curl --location --request POST 'localhost:8080/customer-info/customerorder/save' \
--header 'Content-Type: application/json' \
--data-raw '{
  "customer": {
    "age": 0,
    "id": 1,
    "name": "string",
    "shippingAddress": {
      "city": "string",
      "country": "string",
      "id": 1,
      "streetName": "string"
    }
  },
  "orderDate": "2021-05-05T16:00:35.350Z",
  "orderItems": [
    {
      "quantity": 1
    },
    {
      "quantity": 2
    }
  ],
  "title": "string"
}'
</pre><br/>

Response : 

HTTP response code 200 <br/>
<pre>
{
    "id": 1,
    "orderDate": "2021-05-05T16:00:35.35",
    "customer": {
        "id": 1,
        "name": "string",
        "age": 0,
        "shippingAddress": {
            "id": 1,
            "streetName": "string",
            "city": "string",
            "country": "string"
        }
    },
    "title": "string",
    "orderItems": [
        {
            "id": 1,
            "quantity": 1
        },
        {
            "id": 2,
            "quantity": 2
        }
    ]
}
</pre><br/>

### List OrderItems saved to database

Method : HTTP.GET <br/>
URL : localhost:8080/customer-info/orderitem/list <br/>

Request : 
<pre>
curl --location --request GET 'localhost:8080/customer-info/orderitem/list'
</pre><br/>

Response : 

HTTP response code 200 <br/>
<pre>
[
    {
        "id": 1,
        "quantity": 1
    },
    {
        "id": 2,
        "quantity": 2
    }
]
</pre><br/>
