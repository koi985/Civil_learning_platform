阶段二已经继续落地成可运行闭环了。在阶段一已有基础上，我补完了题库练习、错题本、模考作答、成绩报告、购物车、订单、模拟支付、已购课程和用户中心统计。

本轮新增文档：

docs/stage-2.md

本轮新增数据库表：

shopping_cart_item
course_order
course_order_item
payment_log
user_course
practice_record
practice_answer
user_wrong_question
exam_record
exam_answer

后端新增模块与接口：

QuestionController / QuestionService
ExamCenterController / ExamCenterService
OrderController / OrderService
UserCenterController / UserCenterService

核心接口已经打通：

GET /api/questions
POST /api/practices
GET /api/practices/{id}
POST /api/practices/{id}/submit
GET /api/practices/{id}/report
GET /api/wrong-questions
POST /api/wrong-questions/practice
GET /api/exams
GET /api/exams/{id}
POST /api/exams/{id}/start
GET /api/exam-records/{id}
POST /api/exam-records/{id}/submit
GET /api/exam-records/{id}/report
GET /api/exams/{id}/ranking
GET /api/cart/items
POST /api/cart/items
DELETE /api/cart/items/{id}
POST /api/orders/direct
POST /api/orders/from-cart
GET /api/orders
GET /api/orders/{id}
POST /api/orders/{id}/pay
GET /api/user-center/overview
GET /api/user-center/statistics
GET /api/user-center/courses

前端新增页面与路由：

/question-bank
/practice/:id
/practice/report/:id
/wrong-book
/exam
/exam/detail/:id
/exam/start/:id
/exam/report/:id
/exam/rank/:id
/cart
/my-course
/user/profile
/user/order
/user/stat
/user/vip

同时我重写了公共头部导航并新增了 UserCenterLayout，课程详情页已接入“加入购物车 / 立即下单 / 已购课程”状态。

验证结果已经跑过：

MySQL 已重新执行 sql/init.sql。
后端 mvn -q -DskipTests compile 通过。
前端 npm run build 通过。
后端已重启在 8080，前端 dev 仍在 3000。

我实际跑过一轮 HTTP 级端到端流程：

student1 登录成功
题库列表查询成功
创建练习 -> 拉取题目 -> 提交练习 -> 生成练习报告成功
错题本查询成功，错题再练成功
模考列表 -> 开始模考 -> 提交模考 -> 成绩报告 -> 排行榜成功
购物车下单并支付成功
直接下单并支付成功
用户中心 overview / statistics / courses 成功返回真实数据
前端关键路由 /question-bank /exam /cart /user/profile /my-course 均返回 200

这轮自动测试后，student1 的真实数据已变化：

新增练习记录
新增模考记录
新增并支付了两笔订单
我的课程数量增加到 4

补充说明：

这个环境仍然没有可操作的图形浏览器自动化工具，所以我不能真的在浏览器里逐按钮点完所有页面；我做的是最大范围的前端路由可达性验证和后端 HTTP 级接口联调。

下一阶段建议继续做：

教师端班级作业/考试发布、学生提交、教师批改、成绩报表
后台订单列表 / 支付日志 / 统计页补全
课程播放页、我的课程学习页

---

阶段三已开始落地，当前已经补上：

新增文档：
- `docs/stage-3.md`

新增数据库表：
- `classroom_homework`
- `classroom_homework_question`
- `classroom_homework_submission`
- `classroom_homework_answer`
- `classroom_exam_task`
- `classroom_exam_question`
- `classroom_exam_submission`
- `classroom_exam_answer`

新增后端能力：
- 教师端班级管理与入班审核
- 教师端作业发布 / 班级考试发布
- 班级作业与班级考试提交、报告、排名
- 后台订单列表 / 支付日志 / 统计页
- 用户课程学习播放接口

新增前端页面与路由：
- `/teacher/class`
- `/teacher/homework`
- `/teacher/exam`
- `/teacher/review`
- `/teacher/report`
- `/classroom/homework/start/:id`
- `/classroom/homework/report/:id`
- `/classroom/exam/start/:id`
- `/classroom/exam/report/:id`
- `/admin/order`
- `/admin/payment-log`
- `/admin/stats`
- `/course/play/:id`

本轮已完成验证：
- `mvn -q -DskipTests compile` 通过
- `npm run build` 通过
- MySQL 已重新执行 `sql/init.sql`
- 新版本后端已重新启动在 `8080`
- 已实测学生端班级作业报告、班级考试报告、课程学习接口返回真实数据
- 已实测教师端 dashboard / classrooms / homeworks / classroom-exams / reviews / reports
- 已实测后台 admin orders / payment-logs / stats

当前剩余待继续强化点：
- 教师端表单目前仍使用封面 URL 与题目多选方式，后续可继续升级为更完整的上传与编辑体验
- 班级作业/考试的前端自动化仍是 HTTP 级验证，当前环境没有可操作浏览器自动点击能力
2026-04-10 续推进补充：
- 已修复教师端 `POST /api/teacher/homeworks`、`POST /api/teacher/classroom-exams` 的请求体时间反序列化问题。
- 修复方式：为 `TeacherHomeworkSaveRequest.deadlineAt`、`TeacherClassroomExamSaveRequest.startTime/endTime` 增加 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")`，与前端 `el-date-picker value-format="YYYY-MM-DD HH:mm:ss"` 对齐。
- 已重新执行后端 `mvn -q -DskipTests compile` 与前端 `npm run build`，均通过。
- 已重启后端并完成教师创建作业、教师创建班级考试、学员开始任务、学员提交、教师批改、教师报表、班级详情联调验证。
- 本轮实测结果：
- 新建作业成功，最新作业 ID=`6`
- 新建班级考试成功，最新考试 ID=`2`
- 学员提交新作业成功，submission ID=`3`
- 学员提交新考试成功，submission ID=`3`
- 教师批改新作业/考试成功
- 教师评阅列表记录数=`6`
- 教师报表 `homeworkStats=6`，`examStats=2`
- 班级详情 `homeworks=6`，`classroomExams=2`

2026-04-10 19：12
本轮继续推进的阶段目标，仍然是阶段三里“教师教学管理 + 学生班级学习 + 课程学习播放”的可运行闭环。涉及的数据仍以已有阶段三表为主，这次没有新增表结构，重点对齐了文件链路和联调一致性。接口侧主要覆盖了 GET /api/classrooms/{id}、GET /api/user-center/courses/{id}/learn、GET /api/teacher/classrooms、GET /api/teacher/homeworks、/api/files/upload、/api/files/view/{id}；页面侧主要完善了 /teacher/class、/teacher/homework、/course/detail/:id、/course/play/:id、/classroom/detail/:id 以及后台课程/资讯/教师审核相关页面。

这轮代码已经落到前后端里了。前端把原来散落写死的 http://127.0.0.1:8080 收口成统一 /api 代理，新增了资源地址工具，教师端班级封面和作业附件都改成真实上传链路，课程详情/购课详情/课程学习页的资料下载也都改成走真实文件接口。vite.config.js、request.js、assets.js、TeacherClassPage.vue、TeacherHomeworkPage.vue、CourseLearnPage.vue 已同步更新。后端这边补了班级详情中的作业附件字段返回，方便学生端继续消费。ClassroomService.java

验证结果
mvn -q -DskipTests compile 和 npm run build 都已通过。前端开发服务已起在 3000，我实测 /、/course/detail/1、/course/play/1、/question-bank、/classroom/detail/1、/teacher/class 都返回 200。接口联调方面，student1 / 123456、teacher1 / 123456 登录成功；GET /api/classrooms/1、GET /api/user-center/courses/1/learn、GET /api/teacher/classrooms、GET /api/teacher/homeworks 都返回 200；GET http://127.0.0.1:3000/api/files/view/1 也返回 200，说明前端代理下的文件预览/下载链路已经通了。

下一轮可以直接从“班级详情页把作业附件完整展示出来，以及班级封面进一步升级为 sys_file_record 关联闭环”继续推进。

