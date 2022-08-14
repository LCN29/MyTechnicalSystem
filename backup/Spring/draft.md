    注册 beanDefinition  

    MergedBeanDefinitionPostProcessor
    InstantiationAwareBeanPostProcessor
    BeanPostProcessor
    DestructionAwareBeanPostProcessor


    MergedBeanDefinitionPostProcessor.resetBeanDefinition


    InstantiationAwareBeanPostProcessor

        
    null != InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation  第一个不为null, 就返回


    BeanPostProcessor.postProcessAfterInitialization 全部执行，有一个返回 null， 就返回上一个的返回值, 默认为 null        


    MergedBeanDefinitionPostProcessor.postProcessMergedBeanDefinition 全部执行一遍


    InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation 有一个返回 false, 结束


    InstantiationAwareBeanPostProcessor.postProcessProperties  全部执行一遍

    BeanPostProcessor.postProcessBeforeInitialization  全部执行，有一个返回 null， 就返回上一个的返回值, 默认为 null


    BeanPostProcessor.postProcessAfterInitialization  全部执行，有一个返回 null， 就返回上一个的返回值, 默认为 null


    DestructionAwareBeanPostProcessor.requiresDestruction  全部执行，只要有一个返回 true, 即可


    DestructionAwareBeanPostProcessor.postProcessBeforeDestruction 全部执行一遍