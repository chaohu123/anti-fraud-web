package com.xxx.antifraud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxx.antifraud.common.BusinessException;
import com.xxx.antifraud.common.ErrorCode;
import com.xxx.antifraud.common.enums.LearningStatus;
import com.xxx.antifraud.entity.AntiFraudArticle;
import com.xxx.antifraud.entity.LearningRecord;
import com.xxx.antifraud.mapper.AntiFraudArticleMapper;
import com.xxx.antifraud.mapper.LearningRecordMapper;
import com.xxx.antifraud.service.KnowledgeService;
import com.xxx.antifraud.vo.knowledge.KnowledgeArticleVO;
import com.xxx.antifraud.vo.knowledge.KnowledgeDetailVO;
import com.xxx.antifraud.vo.knowledge.LearningProgressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 防骗知识库与学习进度 Service 实现
 */
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl extends ServiceImpl<AntiFraudArticleMapper, AntiFraudArticle>
        implements KnowledgeService {

    private final LearningRecordMapper learningRecordMapper;

    @Override
    public List<KnowledgeArticleVO> listArticles(String category, String keyword) {
        LambdaQueryWrapper<AntiFraudArticle> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category)) {
            // 前端分类多为中文（如：短信/电话/网站/社交），这里保持原值做精确匹配
            wrapper.eq(AntiFraudArticle::getCategory, category);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AntiFraudArticle::getTitle, keyword)
                    .or().like(AntiFraudArticle::getSummary, keyword));
        }
        wrapper.orderByAsc(AntiFraudArticle::getId);
        List<AntiFraudArticle> list = this.list(wrapper);
        return list.stream().map(this::buildArticleVO).collect(Collectors.toList());
    }

    @Override
    public KnowledgeDetailVO getDetail(Long id) {
        AntiFraudArticle article = this.getById(id);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return buildDetailVO(article);
    }

    @Override
    public LearningProgressVO learn(Long articleId, Long userId, Integer progress) {
        AntiFraudArticle article = this.getById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (progress == null || progress < 0) {
            progress = 0;
        }
        if (progress > 100) {
            progress = 100;
        }
        LearningRecord record = learningRecordMapper.selectOne(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getArticleId, articleId));
        if (record == null) {
            record = new LearningRecord();
            record.setUserId(userId);
            record.setArticleId(articleId);
        }
        record.setProgress(progress);
        // 进度达到 100 视为已完成，否则为学习中
        record.setStatus(progress >= 100 ? LearningStatus.FINISHED.getCode() : LearningStatus.READING.getCode());
        record.setLearnedAt(LocalDateTime.now());
        if (record.getId() == null) {
            learningRecordMapper.insert(record);
        } else {
            learningRecordMapper.updateById(record);
        }

        // 保存后返回最新进度
        return buildProgress(userId);
    }

    @Override
    public LearningProgressVO getProgress(Long userId) {
        return buildProgress(userId);
    }

    /**
     * 根据文章内容构建列表 VO（包含风险等级与防范要点等扩展字段）
     */
    private KnowledgeArticleVO buildArticleVO(AntiFraudArticle article) {
        KnowledgeArticleVO vo = new KnowledgeArticleVO();
        vo.setId(article.getId());
        vo.setCategory(article.getCategory());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());

        // 基于标题/类别的内置知识库，补充风险等级与防范要点
        // 如果后续把字段直接存到数据库，只需要改成从 article 里取即可
        switch (article.getTitle()) {
            case "如何识别钓鱼短信":
                vo.setRiskLevel("高");
                vo.setPreventionTips(Arrays.asList(
                        "不点击短信中的陌生或可疑链接",
                        "不在链接页面输入账号、密码、验证码",
                        "涉及资金、账户异常先通过官方 App/客服电话核实"
                ));
                break;
            case "虚假中奖短信识别":
                vo.setRiskLevel("中");
                vo.setPreventionTips(Arrays.asList(
                        "从未参加过的活动“中奖”一律视为诈骗",
                        "凡是要求先付手续费、税费才能领奖的都是骗局",
                        "中奖信息仅以官网或官方 App 为准"
                ));
                break;
            case "钓鱼邮件防范指南":
                vo.setRiskLevel("高");
                vo.setPreventionTips(Arrays.asList(
                        "仔细核对发件人邮箱域名是否官方",
                        "不要随意打开未知附件或点击邮件中的链接",
                        "账号安全相关操作只在官网或官方 App 完成"
                ));
                break;
            case "识别虚假购物网站":
                vo.setRiskLevel("中");
                vo.setPreventionTips(Arrays.asList(
                        "优先使用知名电商 App/官网，不从陌生链接下单",
                        "检查网址是否为 https 且域名与品牌完全一致",
                        "拒绝脱离平台的私下转账交易"
                ));
                break;
            case "仿冒银行网站识别":
                vo.setRiskLevel("高");
                vo.setPreventionTips(Arrays.asList(
                        "从不通过短信链接或搜索广告登录网银",
                        "核对浏览器安全锁与数字证书，确认正规域名",
                        "页面异常或频繁索要验证码时立即停止操作并致电银行"
                ));
                break;
            case "投资理财诈骗防范":
                vo.setRiskLevel("高");
                vo.setPreventionTips(Arrays.asList(
                        "不相信“高收益、零风险”的投资宣传",
                        "核实平台是否具备正规金融牌照与监管备案",
                        "不在社交群/私聊里给陌生人转账投资"
                ));
                break;
            default:
                // 默认给一个中等风险与通用防范提示，避免前端出现空值
                vo.setRiskLevel("中");
                vo.setPreventionTips(Arrays.asList(
                        "陌生链接与二维码一律谨慎点击或扫描",
                        "不要向陌生人透露验证码、密码等敏感信息"
                ));
                break;
        }
        return vo;
    }

    /**
     * 根据文章内容构建详情 VO（复用列表中的风险等级与防范要点，并补充话术、案例等）
     */
    private KnowledgeDetailVO buildDetailVO(AntiFraudArticle article) {
        KnowledgeDetailVO vo = new KnowledgeDetailVO();
        vo.setId(article.getId());
        vo.setCategory(article.getCategory());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContent(article.getContent());

        // 先复用列表卡片的风险等级与防范要点
        KnowledgeArticleVO articleVO = buildArticleVO(article);
        vo.setRiskLevel(articleVO.getRiskLevel());
        vo.setPreventionTips(articleVO.getPreventionTips());

        // 针对部分知识点补充常见话术、案例与推荐训练
        switch (article.getTitle()) {
            case "如何识别钓鱼短信":
                vo.setCommonTactics(Arrays.asList(
                        "【银行通知】您的账户存在风险，请点击链接及时验证，否则将被冻结",
                        "【快递提醒】您的快递无法投递，请点击链接补充地址或缴纳费用",
                        "【支付平台】您的支付账户异常登录，请点击链接修改密码"
                ));
                vo.setCases(Arrays.asList(
                        "李先生收到自称某支付平台的短信，称其账户异常需要验证身份，并附带一个短链接。李先生点击并在网页中输入账号、密码和短信验证码，结果账户被转走数千元。"
                ));
                vo.setRelatedTraining("短信与验证码安全训练");
                break;
            case "虚假中奖短信识别":
                vo.setCommonTactics(Arrays.asList(
                        "恭喜您成为本次活动幸运用户，请添加客服领取大奖",
                        "您的手机号在全国抽奖活动中中得特等奖，请先缴纳税费后发放",
                        "仅限今天，未在规定时间内办理即视为自动放弃"
                ));
                vo.setCases(Arrays.asList(
                        "王女士收到“电视台抽奖活动”短信称中得轿车一辆，需要先缴纳几千元手续费。她按照短信提示转账后，对方失联，才发现是骗局。"
                ));
                vo.setRelatedTraining("中奖信息真伪识别训练");
                break;
            case "钓鱼邮件防范指南":
                vo.setCommonTactics(Arrays.asList(
                        "伪装成银行或知名网站发送“安全提醒”邮件，附带登录链接",
                        "以“重要文件”“紧急合同”为名发送含恶意程序的附件",
                        "冒充同事或领导发送转账指令邮件"
                ));
                vo.setCases(Arrays.asList(
                        "某公司员工收到冒充财务主管的邮件，要求紧急向指定账户转账项目款数十万元，员工未二次核实直接转账，造成公司重大损失。"
                ));
                vo.setRelatedTraining("邮件与办公场景防骗训练");
                break;
            case "识别虚假购物网站":
                vo.setCommonTactics(Arrays.asList(
                        "通过低价促销链接引导到仿冒购物网站",
                        "要求使用银行转账或第三方收款码付款，拒绝平台担保支付",
                        "以“预售”“内部价”为由让用户提前全额付款"
                ));
                vo.setCases(Arrays.asList(
                        "张先生在社交平台看到某“品牌官方折扣店”链接，页面与正规官网极其相似。他下单并通过转账支付后，迟迟收不到货，联系客服也联系不上。"
                ));
                vo.setRelatedTraining("网购安全与网站真伪识别训练");
                break;
            case "仿冒银行网站识别":
                vo.setCommonTactics(Arrays.asList(
                        "通过搜索广告或短信链接伪装成银行官网登录入口",
                        "提示“安全升级”要求重新输入银行卡号、密码和验证码",
                        "页面整体风格类似银行官网，但细节存在错别字或奇怪元素"
                ));
                vo.setCases(Arrays.asList(
                        "刘先生通过搜索引擎访问到伪造的银行网站，输入网银账号密码及短信验证码后，银行卡账户被连续转出大额资金。"
                ));
                vo.setRelatedTraining("网银登录与资金安全训练");
                break;
            case "投资理财诈骗防范":
                vo.setCommonTactics(Arrays.asList(
                        "以“内部消息”“稳赚不赔项目”诱导投资",
                        "先小额返利建立信任，再诱导用户大量加仓",
                        "以“系统升级”“风控审核”为由限制提现"
                ));
                vo.setCases(Arrays.asList(
                        "陈先生在微信群被拉入“理财交流群”，在群里老师带单指导下前期确实赚了几千元。后来老师让其追加几十万元投资，结果平台突然无法登录，资金全部损失。"
                ));
                vo.setRelatedTraining("投资理财风险识别训练");
                break;
            default:
                // 其他知识点暂不强制补充话术与案例，保持为空即可
                break;
        }
        return vo;
    }

    /**
     * 统计并构建用户学习进度 VO，供 learn / getProgress 复用
     */
    private LearningProgressVO buildProgress(Long userId) {
        // 当前用户所有学习记录
        List<LearningRecord> all = learningRecordMapper.selectList(
                new LambdaQueryWrapper<LearningRecord>().eq(LearningRecord::getUserId, userId));
        long totalCount = this.count();
        List<LearningRecord> finishedList = all.stream()
                .filter(r -> r.getStatus() != null && r.getStatus().equals(LearningStatus.FINISHED.getCode()))
                .collect(Collectors.toList());
        long finishedCount = finishedList.size();
        double completionRate = totalCount == 0 ? 0.0 : finishedCount * 1.0 / totalCount;

        String level;
        if (finishedCount == 0) {
            level = "新手防骗者";
        } else if (finishedCount <= 3) {
            level = "入门守护者";
        } else if (finishedCount <= 6) {
            level = "进阶识骗者";
        } else {
            level = "资深防骗达人";
        }

        LearningProgressVO vo = new LearningProgressVO();
        vo.setUserId(userId);
        vo.setTotalArticles(totalCount);
        vo.setFinishedArticles(finishedCount);
        vo.setCompletionRate(completionRate);
        vo.setLevel(level);
        // 返回已完成的知识ID，用于前端同步“已学习”状态
        vo.setFinishedArticleIds(
                finishedList.stream()
                        .map(LearningRecord::getArticleId)
                        .collect(Collectors.toList())
        );
        return vo;
    }
}

