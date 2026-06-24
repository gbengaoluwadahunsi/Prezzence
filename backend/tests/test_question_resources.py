from services.question_resources import build_question_learn_more, enrich_question


def test_conflict_question_gets_topic_link():
    resources = build_question_learn_more("Tell me about a time you handled a conflict with a colleague.")
    assert "conflict" in resources["learn_more_topic"].lower()
    assert resources["learn_more_url"].startswith("https://")


def test_enrich_question_adds_learn_more_fields():
    question = enrich_question(
        {"text": "How do you prioritize urgent requests?", "type": "behavioral"},
        role_title="Project Coordinator",
    )
    assert question["learn_more_topic"]
    assert question["learn_more_url"].startswith("https://")
