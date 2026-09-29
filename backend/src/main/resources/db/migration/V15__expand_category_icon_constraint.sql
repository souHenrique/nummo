alter table categories
    drop constraint if exists ck_categories_icon;

alter table categories
    add constraint ck_categories_icon
        check (
            icon in (
                'TAG', 'HOME', 'FOOD', 'SHOPPING', 'TRANSPORT',
                'HEALTH', 'EDUCATION', 'LEISURE', 'BILLS', 'TRAVEL',
                'WORK', 'GIFT', 'PET', 'INVESTMENT',
                'RESTAURANT', 'CLOTHING', 'FUEL', 'ENTERTAINMENT',
                'FITNESS', 'MUSIC', 'PHONE', 'INTERNET', 'TECHNOLOGY',
                'COMPUTER', 'SALARY', 'FREELANCE', 'FAMILY', 'CHARITY',
                'OTHER'
            )
        );
